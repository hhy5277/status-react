(ns status-im.ui.components.bottom-sheet.view
  (:require [status-im.ui.components.react :as react]
            [status-im.ui.components.animation :as animation]
            [status-im.ui.components.colors :as colors]
            [reagent.core :as reagent]))

(def initial-animation-duration 300)
(def release-animation-duration 100)
(def cancellation-animation-duration 150)
(def swipe-opacity-range 100)
(def cancellation-height 250)

(def border-radius 16)
(def bottom-padding 34)

(defn animate-sign-panel
  [opacity-value bottom-value]
  (animation/start
   (animation/parallel
    [(animation/timing opacity-value
                       {:toValue  1
                        :duration initial-animation-duration
                        :useNativeDriver true})
     (animation/timing bottom-value
                       {:toValue  0
                        :duration initial-animation-duration
                        :useNativeDriver true})])))

(defn- on-move
  [{:keys [height bottom-value opacity-value]}]
  (fn [_ s]
    (let [dy (.-dy s)]
      (when (pos? dy)
        (let [opacity (max 0.05 (- 1 (/ dy (- height swipe-opacity-range))))]
          (animation/set-value bottom-value dy)
          (animation/set-value opacity-value opacity))))))

(defn cancelled? [height dy vy]
  (or
   (< 0.1 vy)
   (> cancellation-height (- height dy))))

(defn- cancel
  ([opts] (cancel opts nil))
  ([{:keys [height bottom-value show-sheet? opacity-value]} callback]
   (animation/start
    (animation/parallel
     [(animation/timing opacity-value
                        {:toValue  0
                         :duration cancellation-animation-duration
                         :useNativeDriver true})
      (animation/timing bottom-value
                        {:toValue         height
                         :duration        cancellation-animation-duration
                         :useNativeDriver true})])
    #(do (reset! show-sheet? false)
         (animation/set-value bottom-value height)
         (animation/set-value opacity-value 0)
         (when (fn? callback) (callback))))))

(defn- on-release
  [{:keys [height bottom-value opacity-value on-cancel] :as opts}]
  (fn [_ state]
    (let [dy (.-dy state)
          vy (.-vy state)]
      (if (cancelled? height dy vy)
        (cancel opts on-cancel)
        (animation/start
         (animation/parallel
          [(animation/timing opacity-value
                             {:toValue  1
                              :duration release-animation-duration
                              :useNativeDriver true})
           (animation/timing bottom-value
                             {:toValue  0
                              :duration release-animation-duration
                              :useNativeDriver true})]))))))

(defn swipe-pan-responder [opts]
  (.create
   react/pan-responder
   (clj->js
    {:onMoveShouldSetPanResponder (fn [_ state]
                                    (or (< 10 (js/Math.abs (.-dx state)))
                                        (< 5 (js/Math.abs (.-dy state)))))
     :onPanResponderMove          (on-move opts)
     :onPanResponderRelease       (on-release opts)
     :onPanResponderTerminate     (on-release opts)})))

(defn pan-handlers [pan-responder]
  (js->clj (.-panHandlers pan-responder)))

(def container-style
  {:position        :absolute
   :left            0
   :top             0
   :right           0
   :bottom          0
   :flex            1
   :justify-content :flex-end})

(defn shadow-style [opacity-value]
  {:flex             1
   :position         :absolute
   :left             0
   :top              0
   :right            0
   :bottom           0
   :opacity          opacity-value
   :background-color colors/black-transparent-40})

(defn content-container-style
  [content-height bottom-value]
  {:background-color        colors/white
   :border-top-left-radius  border-radius
   :border-top-right-radius border-radius
   :height                  (+ content-height border-radius)
   :align-self              :stretch
   :transform               [{:translateY bottom-value}]
   :justify-content         :flex-start
   :align-items             :center
   :padding-bottom          bottom-padding})

(def content-header-style
  {:height          border-radius
   :align-self      :stretch
   :justify-content :center
   :align-items     :center})

(def handle-style
  {:width            31
   :height           4
   :background-color colors/gray-transparent-40})

(defn- bottom-sheet-view
  [{:keys [opacity-value bottom-value]}]
  (reagent.core/create-class
   {:component-did-mount
    #(animate-sign-panel opacity-value bottom-value)
    :reagent-render
    (fn [{:keys [opacity-value bottom-value
                 height content on-cancel]
          :as   opts}]
      [react/view
       (merge
        (pan-handlers (swipe-pan-responder opts))
        {:style container-style})
       [react/touchable-highlight
        {:on-press on-cancel
         :style    container-style}

        [react/animated-view (shadow-style opacity-value)]]
       [react/animated-view
        {:style (content-container-style height bottom-value)}
        [react/view content-header-style
         [react/view handle-style]]
        content]])}))

(defn bottom-sheet
  [{:keys [show? content-height on-cancel]
    :or   {on-cancel (fn [])}} _]
  (let [show-sheet?          (reagent/atom show?)
        total-content-height (+ content-height border-radius bottom-padding)
        bottom-value         (animation/create-value total-content-height)
        opacity-value        (animation/create-value 0)
        opts                 {:height        total-content-height
                              :bottom-value  bottom-value
                              :opacity-value opacity-value
                              :show-sheet?   show-sheet?
                              :on-cancel     on-cancel}]
    (reagent.core/create-class
     {:component-will-update
      (fn [pr [_ new-args]]
        (let [old-args  (second (.-argv (.-props pr)))
              old-show? (:show? old-args)
              new-show? (:show? new-args)]
          (cond (and (not old-show?) new-show?)
                (reset! show-sheet? true)

                (and old-show? (not new-show?) (true? @show-sheet?))
                (cancel opts))))
      :reagent-render
      (fn [_ content]
        (when @show-sheet?
          [bottom-sheet-view (assoc opts :content content)]))})))
