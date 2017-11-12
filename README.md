# network-utils

A Clojure library designed to ease operations related to networking.

## Usage

Getting the list of available network interfaces (if your system has for instance 4 interfaces called like this: eno1, lo and wlp320):

<code>(get-interfaces-names)</code> produces the following list: <code>'("eno1" "lo" "wlp3s0")</code>

Getting details for a specific interface:

<code>(interface-details "eno1")</code> produces the following map as a result:
<code>
{:name "eno1",
 :type "ether",
 :ip-v4 "192.168.1.9",
 :ip-v6 "aaaa::bbbb:cccc:0000:dddd",
 :mac-address "d0:67:e5:38:71:b0",
 :net-mask "255.255.255.0",
 :gateway "192.168.1.1"}))))
</code>

## License

Copyright © 2017 GNU public Licence V. 3.0