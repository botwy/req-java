package ru.btw.req.views;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ru.btw.req.network.Rest;

import javax.swing.*;
import java.awt.*;
import java.net.http.HttpResponse;

public class MainWindow extends JFrame {

    private JButton button1;

    public MainWindow() {
        setTitle("req");
        setLayout(null); // Using absolute positioning
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        setBounds(dimension.width / 2 - 250, dimension.height / 2 - 500, 500, 600);

        Input urlInput = new Input("url запроса");
        urlInput.setStartPoint(10, 10);
        urlInput.setParent(this);
        urlInput.setValue("http://127.0.0.1:3030/api-get");

        JCheckBox postCheckBox = new JCheckBox("post");
        postCheckBox.setBounds(10, 40, 80, 20);
        add(postCheckBox);
        JLabel rqLabel = new JLabel("тело запроса:");
        rqLabel.setBounds(100, 40, 100, 20);
        add(rqLabel);
        JTextPane rqPane = new JTextPane();
        rqPane.setBounds(10, 70, 465, 160);
        add(rqPane);
        JScrollPane rqScrollPane = new JScrollPane(rqPane);
        rqScrollPane.setBounds(10, 70, 475, 170);
        add(rqScrollPane);
        rqScrollPane.setViewportView(rqPane);

        JLabel cookieLabel = new JLabel("куки:");
        cookieLabel.setBounds(10, 240, 100, 20);
        add(cookieLabel);
        JTextPane cookiePane = new JTextPane();
        cookiePane.setBounds(10, 260, 465, 40);
        add(cookiePane);
        JScrollPane cookieScrollPane = new JScrollPane(cookiePane);
        cookieScrollPane.setBounds(10, 260, 475, 40);
        add(cookieScrollPane);
        cookieScrollPane.setViewportView(cookiePane);

        JLabel statusLabel = new JLabel("статус: ");
        statusLabel.setBounds(10, 310, 150, 20);
        add(statusLabel);

        JTextPane resPane = new JTextPane();
        resPane.setBounds(10, 340, 465, 160);
        add(resPane);
        JScrollPane resScrollPane = new JScrollPane(resPane);
        resScrollPane.setBounds(10, 340, 475, 170);
        add(resScrollPane);
        resScrollPane.setViewportView(resPane);

        Button sendButton = new Button("Отправить");
        sendButton.setBounds(10, 520, 150, 30);
        sendButton.setParent(this);
        sendButton.setPressHandler(() -> {
            String url = urlInput.getValue().trim();
            String cookie = cookiePane.getText().trim();
            HttpResponse<String> res;
            if (postCheckBox.isSelected()) {
                String body = rqPane.getText().trim();
                res = Rest.post(url, body, cookie);
            } else {
                res = Rest.get(url, cookie);
            }
            statusLabel.setText("статус: " + res.statusCode());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(res.body());
            String prettyJsonString = gson.toJson(je);
            resPane.setText(prettyJsonString);
        });
    }
}
