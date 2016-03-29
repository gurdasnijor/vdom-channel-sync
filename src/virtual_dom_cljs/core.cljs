(ns virtual-dom-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [vdom.core :as v]
            [vdom.elm :as e]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(defn todo-item [{:keys [id name checked editing]} dispatch]
  [:li {}
   [:div {:className "view"}
    [:input {
      :className "toggle"
      :type "checkbox"
      :checked checked
      :onclick #(dispatch [:checkitem id (not checked)])}]
    [:label {
      :style {:border "1px solid blue"}
      :ondblclick #(dispatch [:edititem id true])} name]
    [:button {:className "destroy"}]]
   [:input {:className "edit", :value name}]])

(defn items-view [items dispatch]
  [:ul {:className "todo-list"}
      (for [itm items]
        (todo-item itm dispatch))])

(defn todomvc [{:keys [counter, items]} dispatch]
  [:div {}
    [:section {:className "todoapp"}
     [:header {:className "header"}
      [:h1 {} "TODOS"]
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


(defn updatefn [model [action & payload]]
  (js/console.log (clj->js action) (clj->js payload))
  (match action
    :inc (update-in model [:counter] inc)
    :dec (update-in model [:counter] dec)
    :additem (update-in model [:items] #(conj % {:id (count %) :name "New item" :checked false :editing false}))
    :checkitem (update-in model [:items (first payload)] #(assoc % :checked (second payload)))
    :edititem (update-in model [:items (first payload)] #(assoc % :editing (second payload)))
    :no-op model
    x))

(defonce actions-chan (chan))
(def dispatch #(put! actions-chan %))
(def initial-model {:counter 0 :items [{:id 0 :name "Pick up milk" :checked false :editing false}]})
(defonce models-chan (e/foldp updatefn initial-model actions-chan))


(defonce setup
  (e/render! (cljs.core.async/map #(todomvc % dispatch) [models-chan]) (.getElementById js/document "app")))


(defn on-js-reload []
  (put! actions-chan [:no-op]))
