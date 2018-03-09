(ns network-utilities.interface
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]
            [text-tokenization.lines-of-tokens :as m]))

(defn get-interfaces-names
  "Provides a list of interfaces names found in the current system."
  []
  (->> (:out (sh/sh "ifconfig" "-s"))
       (s/split-lines)
       (rest)
       (map #(s/split % #" "))
       (map #(first %))))

(defn- cleanup-string [string]
  (apply str (filter #(or (Character/isLetter %)
                          (Character/isDigit %)) string)))

(defn get-line-in-text-for-interface [routes-text interface-name]
  (->> routes-text
       (clojure.string/split-lines)
       (filter #(clojure.string/includes? % interface-name))
       (filter #(clojure.string/includes? % "default"))
       (first)))

(defn gateway [routes-text]
  (fn [interface-name]
    (or (when-let [line (get-line-in-text-for-interface routes-text interface-name)]
          (-> line
              (s/split #" ")
              (nth 2)))
        "")))

(defn details-for-interface-factory [interface-details-supplier interface-gateway-supplier]
  (fn [interface-name]
      (let [result-matrix (m/to-lines-of-tokens (or (interface-details-supplier interface-name) ""))]
        (if (empty? result-matrix)
          {}
          {:name        (cleanup-string (m/get-token-at 0 0 result-matrix))
           :type        (m/get-token-at 3 0 result-matrix)
           :ip-v4       (m/get-token-at 1 1 result-matrix)
           :ip-v6       (m/get-token-at 2 1 result-matrix)
           :mac-address (m/get-token-at 3 1 result-matrix)
           :net-mask    (m/get-token-at 1 3 result-matrix)
           :gateway     ((gateway (:out (interface-gateway-supplier))) interface-name)}))))

(defn ifconfig-from-shell [interface-name]
  (let [result (sh/sh "ifconfig" interface-name)]
    (if (= (:exit result) 0)
      (:out result)
      "")))

(defn route-for-interface-factory [get-route-using-shell]
  (fn [interface-name]
    (let [result (get-route-using-shell)]
      (if (= (:exit result) 0)
        ((gateway (:out result)) interface-name)
        ""))))

(defn ip-route-shell-command []
  (sh/sh "ip"  "route"))

(def interface-details (details-for-interface-factory ifconfig-from-shell ip-route-shell-command))

(defn- to-binary [number]
  (s/replace (format "%8s" (Integer/toBinaryString (Integer/valueOf number))) #" " "0"))

(defn- amount-of-leading-bits [ones-and-ceroes]
  (or (s/index-of ones-and-ceroes "0") (.length ones-and-ceroes)))

(defn- CIDR-prefix [subnet-mask]
  (str "/"
       (amount-of-leading-bits
         (reduce str
                 (map to-binary (flatten (s/split subnet-mask #"\.")))))))

(defn subnet-mask-to-CIDR [subnet-mask]
  (CIDR-prefix subnet-mask))