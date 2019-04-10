(ns nuid.specs.ethereum
  (:require [clojure.spec.alpha :as s]))

(s/def :nuid.ethereum/config (s/keys :req [:ethereum/http-provider
                                           :ethereum/private-key
                                           :ethereum/coinbase]))

(s/def :ethereum/nil-transaction-id
  #(= % "0x0000000000000000000000000000000000000000000000000000000000000000"))

(s/def :ethereum/transaction-id
  (s/and string?
         not-empty
         #(= (count %) 66)
         #(= (subs % 0 2) "0x")
         #(not (s/valid? :ethereum/nil-transaction-id %))))
