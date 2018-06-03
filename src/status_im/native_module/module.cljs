(ns status-im.native-module.module)

(defprotocol IReactNativeStatus
  (-init-jail [this])
  (-move-to-internal-storage [this callback])
  (-start-node [this config])
  (-stop-node [this])
  (-create-account [this password callback])
  (-recover-account [this passphrase password callback])
  (-login [this address password callback])
  (-approve-sign-request [this id password callback])
  (-approve-sign-request-with-args [this id password gas gas-price callback])
  (-discard-sign-request [this id])
  (-parse-jail [this chat-id file callback])
  (-call-jail [this params])
  (-call-function! [this params])
  (-set-soft-input-mode [this mode])
  (-clear-web-data [this])
  (-call-web3 [this payload callback])
  (-call-web3-private [this payload callback])
  (-module-initialized! [this])
  (-should-move-to-internal-storage? [this callback])
  (-notify-users [this {:keys [message payload tokens] :as m} callback])
  (-add-peer [this enode callback])
  (-close-application [this])
  (-connection-change [this data])
  (-app-state-change [this state])
  (-get-device-UUID [this callback]))

