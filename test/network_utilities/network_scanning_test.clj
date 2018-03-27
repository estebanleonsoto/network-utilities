(ns network-utilities.network-scanning-test
  (:require [clojure.test :refer :all])
  (:require [network-utilities.network-scanning :refer :all]))

(deftest endpoint-check-gets-result-of-valid-ip-address
  (testing "If checking a valid endpoint behind a given ip address, we should obtain the correct result describing the endpoint."
    (is (= (endpoint-check "192.168.1.27") {:host "192.168.1.27"
                                           :port 80
                                           :type "ip-camera"
                                           :stream-path "/live0.264"}))))