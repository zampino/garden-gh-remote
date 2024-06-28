(ns shy-pepper-10
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as server]
            ;; garden-id
            [hiccup.page :as page]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [nextjournal.garden-id :as garden-id]
            ;; garden-email
            [ring.middleware.params :as ring.params]
            [nextjournal.garden-email :as garden-email]
            [nextjournal.garden-email.render :as render-email]
            [nextjournal.garden-email.mock :as mock-email]
            ;; garden-cron
            [nextjournal.garden-cron :as garden-cron]))

(defn html-response [req body]
  (assoc req
         :status 200
         :headers {"content-type" "text/html"}
         :body body))

;; increment a counter every 5 seconds
(defonce counter (atom 0))
(defn scheduled-task [_] (swap! counter inc))
(garden-cron/defcron #'scheduled-task {:second (range 0 60 5)})

(defn cron-fragment []
  [:div.mt-5
   [:a {:href "https://docs.apps.garden/#scheduling-background-tasks"}
    [:h2.bb-greenish-50.w-fit.text-xl "Scheduled tasks"]]
   [:div.mt-2 "Counter has been incremented " [:span.bg-greenish-30.rounded.p-1 @counter] " times, since the application started."]])

;; list persistent storage
(defn ls-storage []
  (.list (io/file (System/getenv "GARDEN_STORAGE"))))

(comment
  ;; test storage locally
  (spit (io/file (System/getenv "GARDEN_STORAGE") "test4.txt") "please persist this"))

(defn storage-fragment []
  [:div.mt-5
   [:a {:href "https://docs.apps.garden/#storage"}
    [:h2.bb-greenish-50.w-fit.text-xl "Storage"]]
   [:div.mt-2 [:p "Persistent storage contains the following files:"]]
   [:ul.mt-2.ml-3.list-disc
    (for [d (ls-storage)]
      [:li.mt-1 [:span.bg-greenish-30.rounded.text-sm.p-1 d]])]])

(defn auth-fragment [req]
  [:div.mt-5
   [:a {:href "https://docs.apps.garden/#user-accounts"}
    [:h2.bb-greenish-50.w-fit.text-xl "User Session"]]
   (if (garden-id/logged-in? req)
     [:div.mt-2
      [:p "You are logged in as:"]
      [:div.bg-greenish-30.rounded.my-1.p-2.overflow-auto {:style {:width "32rem"}}
       (pr-str (garden-id/get-user req))]
      [:a.underline {:href garden-id/logout-uri} "logout"]]
     [:div.mt-2
      [:p "You are not logged in."]
      [:a.underline {:href garden-id/login-uri} "login"]])])

(defn email-fragment [{{:keys [message]} :session}]
  [:div.mt-5
   [:a {:href "https://docs.apps.garden/#email"}
    [:h2.bb-greenish-50.w-fit.text-xl "Email"]]
   [:p.mt-2 "Garden projects allow you to send and receive emails"]
   [:form.mt-2 {:action "/send-email" :method "POST"}
    [:label {:for "to"} "to"]
    [:input.rounded-md.p-1.px-2.block.w-full.border.focus:outline-0.bg-greenish-20.border-none {:name "to" :type "email" :required true}]
    [:label {:for "subject"} "subject"]
    [:input.rounded-md.p-1.px-2.block.w-full.border.focus:outline-0.bg-greenish-20.border-none {:name "subject" :type "text"}]
    [:label {:for "text"} "plain text"]
    [:textarea.rounded-md.p-1.px-2.block.w-full.border.focus:outline-0.bg-greenish-20.border-none {:name "text"}]
    [:label {:for "html"} "html email"]
    [:textarea.rounded-md.p-1.px-2.block.w-full.border.focus:outline-0.bg-greenish-20.border-none {:name "html"}]
    [:button.mt-2.text-center.rounded-md.p-1.px-2.block.w-full.border.focus:outline-0.bg-greenish-30.border-none "send"]]
   (when-some [{:keys [ok error]} message]
     [:div.mt-2.overflow-auto {:style {:width "32rem"}}
      (cond ok [:p.p-3.rounded-md.bg-greenish-30 ok]
            error [:p.p-3.rounded-md.bg-red-500 error])])
   (when garden-email/dev-mode?
     [:div.mt-2.ml-5
      [:a {:href mock-email/outbox-url}
       [:h3.w-fit.bb-greenish-50 "Mock Outbox"]]])
   [:div.mt-2.ml-5
    [:h3.w-fit.bb-greenish-50 "Inbox"]
    [:p.mt-2 "You can send me email at "
     [:a.underline {:href (str "mailto:" garden-email/my-email-address)} garden-email/my-email-address]]
    (render-email/render-mailbox (garden-email/inbox))]])

(defn home-page [req]
  (page/html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "preconnect" :href "https://fonts.bunny.net"}]
    [:link {:rel "preconnect" :href "https://ntk148v.github.io"}]
    (page/include-css "https://fonts.bunny.net/css?family=fira-code:400,600")
    (page/include-css "https://fonts.bunny.net/css?family=inter:400,700")
    (page/include-css "https://ntk148v.github.io/iosevkawebfont/latest/iosevka.css")
    (page/include-js "https://cdn.tailwindcss.com?plugins=typography")
    [:script {:type "importmap"}
     "{\"imports\": {\"squint-cljs/core.js\": \"https://cdn.jsdelivr.net/npm/squint-cljs@0.7.96/src/squint/core.js\"}}"]
    [:script {:type :module :src "https://login.auth.application.garden/leaf.mjs"}]
    [:style {:type "text/css"}
     ":root {
     --greenish: rgba(146, 189, 154, 1);
     --greenish-60: rgba(146, 189, 154, 0.6);
     --greenish-50: rgba(146, 189, 154, 0.5);
     --greenish-30: rgba(146, 189, 154, 0.3);
     --greenish-20: rgba(146, 189, 154, 0.2);
     }
     body { background: #000 !important; font-family: 'Fira Sans', sans-serif; color: var(--greenish); }
     .font-iosevka { font-family: 'Iosevka Web', monospace; }
     .text-greenish { color: var(--greenish); }
     .text-greenish-60 { color: var(--greenish-60); }
     .bg-greenish { background-color: var(--greenish); }
     .bg-greenish-20 { background-color: var(--greenish-20); }
     .bg-greenish-30 { background-color: var(--greenish-30); }
     .border-greenish-50 { border: 4px solid var(--greenish-30); }
     .bb-greenish-30 { border-bottom: 2px solid var(--greenish-30); }
     .bb-greenish-50 { border-bottom: 2px solid var(--greenish-50); }
  "]]
   [:body.flex.text-greenish.justify-center.items-center.font-sans.antialiased.not-prose.text-sm.md:text-base.mt-12
    [:div.flex-col.max-w-md.md:max-w-2xl
     [:div.flex.justify-center
      [:h1.font-iosevka.w-fit.text-greenish.text-3xl.font-light.bb-greenish-50 "Welcome to application.garden!"]]
     [:div#leaf.flex.justify-center.mt-4.md:mt-8
      {:class "h-[100px] md:h-[150px]"
       :style {:stroke "rgba(146, 189, 154, 1)" :stroke-width "0.01"}}]
     [:div.mt-5
      [:p "This is just an example project to get you started with application.garden features, please refer to our "
       [:a.underline {:href "https://docs.apps.garden"} "documentation"] " for more details."]]
     (cron-fragment)
     (storage-fragment)
     (auth-fragment req)
     (email-fragment req)]]))

(defn send-email! [req]
  (let [{:strs [to subject text html]} (:form-params req)]
    (-> req
        (assoc :status 303 :headers {"Location" "/"})
        (update :session assoc :message
                (try {:ok (pr-str
                           (garden-email/send-email! (cond-> {:to {:email to}}
                                                       (not= "" subject) (assoc :subject subject)
                                                       (not= "" text) (assoc :text text)
                                                       (not= "" html) (assoc :html html))))}
                     (catch Exception e {:error (ex-message e)}))))))

(defn app [{:as req :keys [request-method uri]}]
  (case [request-method uri]
    ;; application.garden pings your project with a HEAD request at `/` to know whether it successfully started
    [:head "/"] {:status 202}
    [:post "/send-email"] (send-email! req)
    [:get "/"] (-> req
                   (update :session dissoc :message)
                   (html-response (home-page req)))
    {:status 404 :body "not found"}))

(def wrapped-app
  (-> app
      ;; garden-email
      (ring.params/wrap-params)
      (garden-email/wrap-with-email #_{:on-receive (fn [email] (println "Got mail"))})
      ;; garden-id
      (garden-id/wrap-auth #_{:github [{:team "nextjournal"}]})
      (session/wrap-session {:store (cookie-store)})))

(defn start! [opts]
  (let [server (server/run-server #'wrapped-app
                                  (merge {:legacy-return-value? false
                                          :host "0.0.0.0"
                                          :port 7777}
                                         opts))]
    (println (format "server started on port %s"
                     (server/server-port server)))))
