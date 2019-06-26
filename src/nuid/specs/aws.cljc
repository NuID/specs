(ns nuid.specs.aws
  (:require [clojure.spec.alpha :as s]))

(s/def ::sqs-submit-response
  (s/keys :req-un [::MessageId ::MD5OfMessageBody]))

(s/def ::sns-submit-response
  (s/keys :req-un [::MessageId]))
