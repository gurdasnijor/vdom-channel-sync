(ns virtual-dom-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [vdom.core :as v]
            [vdom.elm :as e]
            [cognitect.transit :as t]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(def json-reader (t/reader :json))
(def json-writer (t/writer :json))


(defonce ws-chan (atom nil))
(defonce actions-chan (chan))
; (def dispatch #(put! actions-chan %))


(defn receive-transit-msg!
 [update-fn]
 (fn [msg]
   (update-fn
     (->> msg .-data (t/read json-reader)))))

(defn send-transit-msg!
 [msg]
 (prn msg)
 (if @ws-chan
   (.send @ws-chan (t/write json-writer msg))
   (throw (js/Error. "Websocket is not available!"))))

(defn make-websocket! [url receive-handler]
 (println "attempting to connect websocket")
 (if-let [chan (js/WebSocket. url)]
   (do
     (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
     (reset! ws-chan chan)
     (println "Websocket connection established with: " url))
   (throw (js/Error. "Websocket connection failed!"))))


(def dispatch #(send-transit-msg! %))

; (defn get-messages! [data]
;   (js/console.log (clj->js data)))

(defn get-messages! [data]
  (put! actions-chan data))




(defn todo-item [{:keys [id name checked editing]} dispatch]
  [:li {:className (if editing "editing" "")}
   [:div {:className "view"}
    [:input {
      :className "toggle"
      :type "checkbox"
      :checked checked
      :onclick #(dispatch [:checkitem id (not checked)])}]
    [:label {
      :style {:border "1px solid blue"}
      :ondblclick #(dispatch [:edititem id true])} name]
    [:button {
      :className "destroy"
      :onclick #(dispatch [:deleteitem id])}]]
   [:input {:className "edit", :value name}]])

(defn items-view [items dispatch]
  [:ul {:className "todo-list"}
      (for [itm items]
        (todo-item itm dispatch))])

(defn todomvc [{:keys [counter, items]} dispatch]
  [:div {}
    [:section {:className "todoapp"}
     [:header {:className "header"}
      [:h1 {} "DISTRIBUTED"]
      [:input {
        :className "new-todo"
        :placeholder "What needs to be done?"
        :autofocus true
        :onkeydown #(dispatch [:additem]) }]]
     [:section {:className "main"}
      [:input {:className "toggle-all", :type "checkbox"}]
      [:label {:for "toggle-all"} "Mark all as complete"]
      (items-view items dispatch)]
     [:footer {:className "footer"}
      [:span {:className "todo-count"}
       [:strong {} "0"]" item left"]
      [:ul {:className "filters"}
       [:li
        [:a {:className "selected", :href "#/"} "All"]]
       [:li
        [:a {:href "#/active"} "Active"]]
       [:li
        [:a {:href "#/completed"} "Completed"]]]
      [:button {:className "clear-completed"} "Clear completed"]]]])


(defn updatefn [model action]
  (match action
    [:additem              ] (update-in model [:items] #(conj % {:id (count %) :name "New item" :checked true :editing false}))
    [:checkitem  id checked] (update-in model [:items id] #(assoc % :checked checked))
    [:edititem   id editing] (update-in model [:items id] #(assoc % :editing editing))
    [:deleteitem id        ] (update-in model [:items id] #(assoc % :editing true))
    :else model
    x))


(def initial-model {:counter 0 :items [{:id 0 :name "Pick up milk" :checked false :editing false}]})
(defonce models-chan (e/foldp updatefn initial-model actions-chan))


(defonce setup
  (e/render! (cljs.core.async/map #(todomvc % dispatch) [models-chan]) (.getElementById js/document "app")))

(defonce setupws
  (make-websocket! (str "ws://localhost:9777/ws") get-messages!))


(defn on-js-reload []
  (put! actions-chan [:no-op]))
