(ns network-utilities.network-scanning
  (:require [org.httpkit.client :as http]
            [network-utilities.interface :as interface]
            [clojure.network.ip :as ip]))

(defn endpoint-check [ip-address]
  (let [response @(http/get (str "http://" ip-address "/ping.html"))]
    (if (= (:status response) 200)
      {:host ip-address
       :port 80
       :type "ip-camera"
       :stream-path "/live0.264"}
      {})))

(defn network-check [ip-addresses]
  (map endpoint-check ip-addresses))

(defn get-cameras []
  (network-check
    (ip/make-network
      (interface/CIDR-for-interface
        (interface/interface-details "eno1")))))