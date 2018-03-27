(ns network-utilities.interface-test
  (:require [clojure.test :refer :all]
            [network-utilities.interface :refer :all]))

(deftest get-interfaces-names-gets-the-interface-names-in-the-system
  (testing "This should return the list of interface names found using \"ifconfig -s\"."
    (is (= (get-interfaces-names) '("docker0" "eno1" "lo" "wlp3s0")))))

(defn mocked-details-supplier [interface-name]
  "eno1: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n        inet 192.168.1.10  netmask 255.255.255.0  broadcast 192.168.1.255\n        inet6 fe80::42a2:ac3e:b51d:dd3f  prefixlen 64  scopeid 0x20<link>\n        ether d0:67:e5:38:71:b0  txqueuelen 1000  (network-utilities)\n        RX packets 267640  bytes 344188453 (344.1 MB)\n        RX errors 30231  dropped 0  overruns 0  frame 16710\n        TX packets 229249  bytes 20470260 (20.4 MB)\n        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0\n        device interrupt 20  memory 0xef500000-ef520000")

(defn mocked-route-supplier []
  {:exit 0
   :out "default via 192.168.1.1 dev eno1 proto static metric 100 \ndefault via 192.168.1.1 dev wlp3s0 proto static metric 600 \n169.254.0.0/16 dev wlp3s0 scope link metric 1000 \n172.17.0.0/16 dev docker0 proto kernel scope link src 172.17.0.1 linkdown \n192.168.1.0/24 dev eno1 proto kernel scope link src 192.168.1.9 metric 100 \n192.168.1.0/24 dev wlp3s0 proto kernel scope link src 192.168.1.8 metric 600 \n"
   :err ""})

(defn mocked-empty-details-supplier [interface-name]
  "")

(deftest get-interface-details-test
  (testing "This should get a map with the details about eno1"
    (let [result ((details-for-interface-factory mocked-details-supplier mocked-route-supplier) "eno1")]
      (is (= (:name result) "eno1"))
      (is (= (:type result) "ether"))
      (is (= (:ip-v4 result) "192.168.1.10"))
      (is (= (:ip-v6 result) "fe80::42a2:ac3e:b51d:dd3f"))
      (is (= (:mac-address result) "d0:67:e5:38:71:b0"))
      (is (= (:net-mask result) "255.255.255.0"))
      (is (= (:gateway result) "192.168.1.1")))))

(deftest get-interface-details-test-of-non-existing-interface-returns-nil
  (testing "This should get a map with the details about eno1"
    (is (= ((details-for-interface-factory mocked-empty-details-supplier mocked-route-supplier) "DOESNt EXIST")
           {}))))


(deftest route-for-interface-factory-test
  (testing "This should get the ip address for eno1"
    (is (= ((route-for-interface-factory mocked-route-supplier) "eno1") "192.168.1.1"))))

(def sample-ip-route-output
  "default via 192.168.1.1 dev eno1 proto static metric 100 \ndefault via 192.168.1.1 dev wlp3s0 proto static metric 600 \n169.254.0.0/16 dev wlp3s0 scope link metric 1000 \n172.17.0.0/16 dev docker0 proto kernel scope link src 172.17.0.1 linkdown \n192.168.1.0/24 dev eno1 proto kernel scope link src 192.168.1.9 metric 100 \n192.168.1.0/24 dev wlp3s0 proto kernel scope link src 192.168.1.8 metric 600")

(deftest gateway-gets-ip-for-valid-interface-test
  (testing "Should return the gateway ip for interface eno1"
    (is (= ((gateway sample-ip-route-output) "eno1") "192.168.1.1"))))

(deftest gateway-gets-empty-string-for-invalid-interface-test
  (testing "Should return the gateway ip for interface eno1"
    (is (= ((gateway sample-ip-route-output) "doesntExist") ""))))


(deftest get-line-in-text-for-interface-test
  (testing ""
    (is (= (get-line-in-text-for-interface sample-ip-route-output "eno1") "default via 192.168.1.1 dev eno1 proto static metric 100 "))))

(deftest interface-details-runs
  (testing "The command interface-details for local"
    (is (= (interface-details "lo")
           {:name "lo"
            :type "loop"
            :ip-v4 "127.0.0.1"
            :ip-v6 "::1"
            :mac-address "txqueuelen"
            :net-mask "255.0.0.0"
            :gateway ""}))))

(deftest interface-details-runs
  (testing "The command interface-details for eno1"
    (is (= (interface-details "eno1")
           {:name "eno1"
            :type "ether"
            :ip-v4 "192.168.1.2"
            :ip-v6 "fe80::42a2:ac3e:b51d:dd3f"
            :mac-address "d0:67:e5:38:71:b0"
            :net-mask "255.255.255.0"
            :gateway "192.168.1.1"}))))

;(deftest interface-details-test
;  (testing "Calling interface-details for real"
;    (is (= (interface-details "eno1") {:name "eno1", :type "ether", :ip-v4 "192.168.1.9", :ip-v6 "fe80::42a2:ac3e:b51d:dd3f", :mac-address "d0:67:e5:38:71:b0", :net-mask "255.255.255.0", :gateway "192.168.1.1"}))))

(deftest get-interfaces-names-test
  (testing "Calling get-interface-names for real"
    (is (= (get-interfaces-names) '("docker0" "eno1" "lo" "wlp3s0")))))


(deftest subnet-mask-to-CIDR-test
  (testing "subnet-mask-to-CIDR must convert from this format 255.255.255.0 to /24"
    (doseq [test-data '({:input "255.0.0.0" :expected-result "/8"}
                        {:input "255.255.255.0" :expected-result "/24"}
                        {:input "255.255.0.0" :expected-result "/16"}
                        {:input "255.255.255.128" :expected-result "/25"}
                        {:input "255.255.1.0" :expected-result "/16"} )]
      (is (= (subnet-mask-to-CIDR (:input test-data)) (:expected-result test-data))))))

(deftest CIDR-for-interface-test
  (testing "Should get a CIDR in the form of 192.168.1.1/24 for network with gateway 192.168.1.1 and subnet mask of 255.255.255.0"
    (is (= (CIDR-for-interface {:name "eno1"
                                :type "ether"
                                :ip-v4 "192.168.1.2"
                                :ip-v6 "fe80::42a2:ac3e:b51d:dd3f"
                                :mac-address "d0:67:e5:38:71:b0"
                                :net-mask "255.255.255.0"
                                :gateway "192.168.1.1"}) "192.168.1.1/24"))))