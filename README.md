Markov-chain generator in Clojure. You can use it as a library or from the command line.

It is the brains behind [Scrumdiddlyumptious Clojure](http://scrumdiddlyumptious-clojure.tumblr.com/).

##Comand-Line Usage
```shell
# to generate text based on some training material
lein run input-1.txt input-2.txt

# to generate a chain to be used later
lein run input-1.txt input-2.txt output.edn

# to generate text based on an existing chain
lein run output.edn
```
