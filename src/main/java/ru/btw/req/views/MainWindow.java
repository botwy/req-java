package ru.btw.req.views;

import com.google.gson.*;
import ru.btw.req.network.Rest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.http.HttpResponse;

public class MainWindow extends JFrame {
    private int inset = 100;

    public MainWindow() {
        setTitle("req");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Получаем размеры экрана
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        setBounds(inset, inset, dimension.width - inset * 2, dimension.height - inset * 2);
        setResizable(true);

        // Основная панель с отступами
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель для URL
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel urlLabel = new JLabel("url запроса:");
        JTextField urlField = new JTextField("http://127.0.0.1:3030/api-get");

        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        // Панель для метода запроса
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        methodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JCheckBox postCheckBox = new JCheckBox("POST");
        JLabel rqLabel = new JLabel("тело запроса:");

        methodPanel.add(postCheckBox);
        methodPanel.add(rqLabel);

        // Панель для тела запроса
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JTextPane rqPane = new JTextPane();
        JScrollPane rqScrollPane = new JScrollPane(rqPane);
        rqScrollPane.setPreferredSize(new Dimension(0, 170));

        requestPanel.add(rqScrollPane, BorderLayout.CENTER);

        // Панель для куков
        JPanel cookiePanel = new JPanel(new BorderLayout());
        cookiePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        JLabel cookieLabel = new JLabel("куки:");
        JTextPane cookiePane = new JTextPane();
        JScrollPane cookieScrollPane = new JScrollPane(cookiePane);
        cookieScrollPane.setPreferredSize(new Dimension(0, 140));

        cookiePanel.add(cookieLabel, BorderLayout.NORTH);
        cookiePanel.add(cookieScrollPane, BorderLayout.CENTER);

        // Панель статуса
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel statusLabel = new JLabel("статус: ");
        statusPanel.add(statusLabel);

        // Панель для ответа
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        JTextPane resPane = new JTextPane();
        JScrollPane resScrollPane = new JScrollPane(resPane);
        resScrollPane.setPreferredSize(new Dimension(0, 200));

        responsePanel.add(new JLabel("Ответ:"), BorderLayout.NORTH);
        responsePanel.add(resScrollPane, BorderLayout.CENTER);

        // Кнопка отправки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton sendButton = new JButton("Отправить");
        sendButton.setPreferredSize(new Dimension(150, 30));
        buttonPanel.add(sendButton);

        // Добавляем все панели
        mainPanel.add(urlPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(methodPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(requestPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(cookiePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(responsePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);

        // Обработчик кнопки
        sendButton.addActionListener((ActionEvent e) -> {
            String url = urlField.getText().trim();
            String cookie = cookiePane.getText().trim();
            HttpResponse<String> res;

            try {
                if (postCheckBox.isSelected()) {
                    String body = rqPane.getText().trim();
                    res = Rest.post(url, body, cookie);
                } else {
                    res = Rest.get(url, cookie);
                }

                statusLabel.setText("статус: " + res.statusCode());

                try {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonElement je = JsonParser.parseString(res.body());
                    String prettyJsonString = gson.toJson(je);
                    resPane.setText(prettyJsonString);
                } catch (JsonSyntaxException ex) {
                    resPane.setText(res.body()); // Если не JSON, показываем как есть
                }

            } catch (Exception ex) {
                statusLabel.setText("статус: ошибка");
                resPane.setText("Ошибка: " + ex.getMessage());
            }
        });

        add(mainPanel);
        setVisible(true);
    }
}