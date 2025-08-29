package ru.btw.req.views;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Button extends JButton implements ActionListener {
    private PressHandler pressHandler;

    public Button(String title) {
        super(title);
        addActionListener(this);
    }

    public void setPressHandler(PressHandler pressHandler) {
        this.pressHandler = pressHandler;
    }

    public void setParent(JFrame parent) {
        parent.add(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pressHandler.handle();
    }
}
