package ru.btw.req.views;

import javax.swing.*;

public class Input {
    private JTextField textField;
    private JLabel label;
    private int labelWidth = 100;
    private int fieldWidth = 370;

    public Input(String title) {
        label = new JLabel(title);
        textField = new JTextField();
    }

    public void setStartPoint(int x, int y) {
        label.setBounds(x, y, labelWidth, 20);
        textField.setBounds(x + labelWidth + 10, y, fieldWidth, 20);
    }

    public void setWidth(int width) {
        textField.setBounds(textField.getX(), textField.getY(), width, 20);
    }

    public void setParent(JFrame parent) {
        parent.add(label);
        parent.add(textField);
    }

    public String getValue() {
        return textField.getText();
    }

    public void setValue(String value) {
        textField.setText(value);
    }
}
