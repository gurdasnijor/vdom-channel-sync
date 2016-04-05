(ns virtual-dom-cljs.server.core
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [org.httpkit.server :as server])
  (:gen-class))


(defonce channels (atom #{}))

(defn notify-clients [msg]
  (doseq [channel @channels]
      (server/send! channel msg)))

(defn connect! [channel]
  (println "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (println "channel closed:" status)
  (swap! channels #(remove #{channel} %)))


(defn handle-websocket [request]
  (server/with-channel request channel
    (connect! channel)
    (server/on-close channel (partial disconnect! channel))
    (server/on-receive channel #(notify-clients %))))


(defroutes routes
  (GET "/ws" [] handle-websocket))

(def application (handler/site routes))

(defn -main [& _]
  (let [port (-> (System/getenv "SERVER_PORT")
                 (or "9777")
                 (Integer/parseInt))]
    (server/run-server application {:port port, :join? false})
    (println "Listening for connections on port" port)))
