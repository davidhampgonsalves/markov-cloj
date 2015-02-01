Markov-chain generator in Clojure. You can use it as a library or from the command line. It also can post said text to tumblr.

It is the brains behind [Scrumdiddlyumptious Clojure](http://scrumdiddlyumptious-clojure.tumblr.com/).

##Comand-Line Usage
```shell
# to generate text based on some training material
lein with-profile markov run input-1.txt input-2.txt

# to generate a chain to be used later
lein with-profile markov run input-1.txt input-2.txt output.edn

# to generate text based on an existing chain
lein with-profile markov run output.edn

# to post a formatted message to tumblr
lein with-profile tumblr run input.edn
```

##Schedule Execution
mv markov-tumblr.plist ~/Library/LauchAgents
sudo chmod 600 ~/Library/LaunchAgents/markov-tumblr.plist
sudo chown root ~/Library/LaunchAgents/markov-tumblr.plist
sudo launchctl load -w ~/Library/LaunchAgents/markov-tumblr.plist
