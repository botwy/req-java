package ru.btw.req.views;

import com.google.gson.*;
import ru.btw.req.network.Rest;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.http.HttpResponse;

public class MainWindow extends JFrame {
    private int inset = 100;
    private Highlighter.HighlightPainter searchHighlighter;
    private java.util.List<Integer> searchPositions = new java.util.ArrayList<>();
    private int currentSearchIndex = -1;
    private JLabel searchResultLabel;

    public MainWindow() {
        setTitle("req");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Инициализируем подсветку поиска
        searchHighlighter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        // Получаем размеры экрана
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        setBounds(inset, inset, dimension.width - inset * 2, dimension.height - inset * 2);
        setResizable(true);

        // Основная панель с BorderLayout - кнопка будет внизу
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Вертикальная панель для всех компонентов кроме кнопки
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

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
        requestPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JTextPane rqPane = new JTextPane();
        JScrollPane rqScrollPane = new JScrollPane(rqPane);
        rqScrollPane.setPreferredSize(new Dimension(0, 150));

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

        // Панель для ответа с поиском
        JPanel responsePanel = new JPanel(new BorderLayout());

        JTextPane resPane = new JTextPane();
        JScrollPane resScrollPane = new JScrollPane(resPane);
        resScrollPane.setPreferredSize(new Dimension(0, 0));

        // Панель поиска
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel searchLabel = new JLabel("Ответ. Поиск:");
        JTextField searchField = new JTextField(45);
        JButton searchButton = new JButton("Найти");
        JButton clearSearchButton = new JButton("Очистить");
        JButton nextButton = new JButton("Следующий");
        JButton prevButton = new JButton("Предыдущий");
        this.searchResultLabel = new JLabel("");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        searchPanel.add(prevButton);
        searchPanel.add(nextButton);
        searchPanel.add(this.searchResultLabel);

        responsePanel.add(searchPanel, BorderLayout.NORTH);
        responsePanel.add(resScrollPane, BorderLayout.CENTER);

        // Добавляем все панели в contentPanel
        contentPanel.add(urlPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(methodPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(requestPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(cookiePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(statusPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(responsePanel);

        // Кнопка отправки - отдельная панель для привязки к низу
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton sendButton = new JButton("Отправить");
        sendButton.setPreferredSize(new Dimension(150, 30));
        buttonPanel.add(sendButton);

        // Добавляем contentPanel и buttonPanel в mainPanel
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Обработчик кнопки отправки
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

        // Обработчик кнопки поиска
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                searchInTextPane(resPane, resScrollPane, searchText);
            }
        });

        // Обработчик очистки поиска
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            searchResultLabel.setText("");
            clearSearchHighlights(resPane);
            searchPositions.clear();
            currentSearchIndex = -1;
        });

        // Обработчик кнопки "Следующий"
        nextButton.addActionListener(e -> {
            if (!searchPositions.isEmpty()) {
                currentSearchIndex = (currentSearchIndex + 1) % searchPositions.size();
                scrollToPosition(resPane, resScrollPane, searchPositions.get(currentSearchIndex));
                showSearchStatus();
            }
        });

        // Обработчик кнопки "Предыдущий"
        prevButton.addActionListener(e -> {
            if (!searchPositions.isEmpty()) {
                currentSearchIndex = (currentSearchIndex - 1 + searchPositions.size()) % searchPositions.size();
                scrollToPosition(resPane, resScrollPane, searchPositions.get(currentSearchIndex));
                showSearchStatus();
            }
        });

        // Поиск при нажатии Enter в поле поиска
        searchField.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                searchInTextPane(resPane, resScrollPane, searchText);
            }
        });

        add(mainPanel);
        setVisible(true);
    }

    // Метод для поиска текста в JTextPane с автоскроллом
    private void searchInTextPane(JTextPane textPane, JScrollPane scrollPane, String searchText) {
        clearSearchHighlights(textPane);
        searchPositions.clear();
        currentSearchIndex = -1;

        String content = textPane.getText();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Текст ответа пуст",
                    "Поиск",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Highlighter highlighter = textPane.getHighlighter();
        String searchTextLower = searchText.toLowerCase();
        String contentLower = content.toLowerCase();

        int index = 0;
        int foundCount = 0;

        try {
            // Находим все вхождения и сохраняем позиции
            while ((index = contentLower.indexOf(searchTextLower, index)) >= 0) {
                int endIndex = index + searchText.length();
                highlighter.addHighlight(index, endIndex, searchHighlighter);
                searchPositions.add(index);
                foundCount++;
                index = endIndex;
            }

            // Автоскролл к первому найденному элементу
            if (foundCount > 0) {
                currentSearchIndex = 0;
                scrollToPosition(textPane, scrollPane, searchPositions.get(0));
                showSearchStatus();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Текст не найден: " + searchText,
                        "Поиск",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (BadLocationException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при поиске: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Метод для прокрутки к указанной позиции
    private void scrollToPosition(JTextPane textPane, JScrollPane scrollPane, int position) {
        try {
            // Устанавливаем курсор на найденную позицию
            textPane.setCaretPosition(position);
            textPane.moveCaretPosition(position + 1); // Выделяем первый символ

            // Получаем прямоугольник позиции текста
            Rectangle textRect = textPane.modelToView(position);
            if (textRect != null) {
                // Получаем видимую область скроллпанели
                Rectangle viewRect = scrollPane.getViewport().getViewRect();

                // Вычисляем новую позицию для прокрутки
                int y = textRect.y - (viewRect.height / 3); // Прокручиваем так, чтобы текст был в верхней трети

                // Устанавливаем новую позицию прокрутки
                scrollPane.getViewport().setViewPosition(new Point(0, y));
            }
        } catch (BadLocationException ex) {
            // Если не удалось получить прямоугольник, просто устанавливаем позицию курсора
            textPane.setCaretPosition(position);
        }
    }

    // Метод для отображения статуса поиска
    private void showSearchStatus() {
        if (!searchPositions.isEmpty()) {
            this.searchResultLabel.setText((currentSearchIndex + 1) + "/" + searchPositions.size());
        }
    }

    // Метод для очистки подсветки поиска
    private void clearSearchHighlights(JTextPane textPane) {
        Highlighter highlighter = textPane.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();

        for (Highlighter.Highlight highlight : highlights) {
            if (highlight.getPainter() == searchHighlighter) {
                highlighter.removeHighlight(highlight);
            }
        }
    }
}