package com.example.touchkit;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.vaadin.addon.touchkit.annotations.OfflineModeEnabled;
import com.vaadin.addon.touchkit.ui.NavigationButton;
import com.vaadin.addon.touchkit.ui.NavigationManager;
import com.vaadin.addon.touchkit.ui.NavigationView;
import com.vaadin.addon.touchkit.ui.NumberField;
import com.vaadin.addon.touchkit.ui.Switch;
import com.vaadin.addon.touchkit.ui.VerticalComponentGroup;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

@Theme("touchkit")
@Title("TouchKit on Jakarta EE 10")
@Push
@OfflineModeEnabled(false)
public class MobileUI extends UI {

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "push-demo");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> clockTask;

    @Override
    protected void init(VaadinRequest request) {
        NavigationManager manager = new NavigationManager();
        manager.setCurrentComponent(createMainView(manager));
        setContent(manager);
    }

    private NavigationView createMainView(NavigationManager manager) {
        VerticalComponentGroup info = new VerticalComponentGroup("Environment");
        info.addComponent(new Label("Java: " + System.getProperty("java.version")));
        info.addComponent(new Label("Server: "
                + VaadinServlet.getCurrent().getServletContext().getServerInfo()));
        info.addComponent(new Label("Servlet API: "
                + VaadinServlet.getCurrent().getServletContext().getMajorVersion() + "."
                + VaadinServlet.getCurrent().getServletContext().getMinorVersion()));

        VerticalComponentGroup form = new VerticalComponentGroup("Foo Bar");
        TextField name = new TextField("Name");
        NumberField amount = new NumberField("Amount");
        Switch urgent = new Switch("Urgent");
        form.addComponents(name, amount, urgent);

        Button save = new Button("Save", e -> Notification.show("Saved " + name.getValue()
                + ", amount " + amount.getValue() + ", urgent=" + urgent.getValue()));

        VerticalComponentGroup nav = new VerticalComponentGroup("Navigation");
        nav.addComponent(new NavigationButton("Server push demo", createPushView()));

        NavigationView view = new NavigationView("TouchKit + Jakarta");
        view.setContent(new com.vaadin.ui.CssLayout(info, form, save, nav));
        return view;
    }

    private NavigationView createPushView() {
        Label clock = new Label("-");
        Button start = new Button("Start clock (server push)", e -> {
            if (clockTask == null) {
                clockTask = TIMER.scheduleAtFixedRate(() -> access(() -> clock.setValue(
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME))),
                        0, 1, TimeUnit.SECONDS);
            }
        });
        VerticalComponentGroup group = new VerticalComponentGroup("Push");
        group.addComponents(clock, start);
        NavigationView view = new NavigationView("Push");
        view.setContent(group);
        return view;
    }

    @Override
    public void detach() {
        if (clockTask != null) {
            clockTask.cancel(true);
        }
        super.detach();
    }
}
