package com.example.touchkit;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;

@WebServlet(urlPatterns = "/*", name = "AppServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MobileUI.class, productionMode = false,
        widgetset = "com.example.touchkit.AppWidgetset")
public class AppServlet extends JakartaTouchKitServlet {
}
