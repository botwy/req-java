package ru.btw.req.views;

import com.google.gson.*;
import ru.btw.req.network.Rest;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private static final int INSET = 100;

    private Highlighter.HighlightPainter searchHighlighter;
    private java.util.List<Integer> searchPositions = new java.util.ArrayList<>();
    private int currentSearchIndex = -1;
    private JLabel searchResultLabel;

    // Компоненты для хранения истории
    private JTextField urlField;
    private JTextPane rqPane;
    private JTextPane cookiePane;
    private JCheckBox postCheckBox;

    // Файлы конфигурации
    private static final String CONFIG_DIR =
            System.getProperty("user.home") + File.separator + ".req-java";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    private static final String HEADERS_FILE = CONFIG_DIR + File.separator + "headers.json";
    private List<RequestConfig> requestHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    // Дефолтные хедеры
    private DefaultHeaders defaultHeaders = new DefaultHeaders();

    private void initializeConfigPaths() {
        // Создаем директорию немедленно
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            try {
                dir.mkdirs();
                // Устанавливаем права
                dir.setReadable(true, false);
                dir.setWritable(true, false);
                dir.setExecutable(true, false);
            } catch (SecurityException e) {
                System.out.println("Ошибка создании папки " + CONFIG_DIR + " " + e.getMessage());
            }
        }
    }

    public MainWindow() {
        setTitle("req");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Инициализируем подсветку поиска
        searchHighlighter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        // Загружаем историю запросов и дефолтные хедеры
        initializeConfigPaths();
        loadConfig();
        loadDefaultHeaders();

        // Получаем размеры экрана
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        setBounds(INSET, INSET, dimension.width - INSET * 2, dimension.height - INSET * 2);
        setResizable(true);

        // Основная панель с BorderLayout - кнопка будет внизу
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Вертикальная панель для всех компонентов кроме кнопки
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Панель для URL с историей
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel urlLabel = new JLabel("url запроса:");
        urlField = new JTextField();

        // Кнопка выбора из истории
        JButton historyButton = new JButton("История");
        historyButton.setPreferredSize(new Dimension(80, 20));

        JPanel urlFieldPanel = new JPanel(new BorderLayout());
        urlFieldPanel.add(urlField, BorderLayout.CENTER);
        urlFieldPanel.add(historyButton, BorderLayout.EAST);

        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlFieldPanel, BorderLayout.CENTER);

        // Панель для метода запроса
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        methodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        postCheckBox = new JCheckBox("POST");
        JLabel rqLabel = new JLabel("тело запроса:");

        methodPanel.add(postCheckBox);
        methodPanel.add(rqLabel);

        // Панель для тела запроса
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        rqPane = new JTextPane();
        JScrollPane rqScrollPane = new JScrollPane(rqPane);
        rqScrollPane.setPreferredSize(new Dimension(0, 150));

        requestPanel.add(rqScrollPane, BorderLayout.CENTER);

        // Панель для куков с кнопкой управления хедерами
        JPanel cookiePanel = new JPanel(new BorderLayout());
        cookiePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        JPanel cookieHeaderPanel = new JPanel(new BorderLayout());
        JLabel cookieLabel = new JLabel("куки:");
        JButton manageHeadersButton = new JButton("Управление хедерами");
        manageHeadersButton.setPreferredSize(new Dimension(160, 20));

        cookieHeaderPanel.add(cookieLabel, BorderLayout.WEST);
        cookieHeaderPanel.add(manageHeadersButton, BorderLayout.EAST);

        cookiePane = new JTextPane();
        // Устанавливаем дефолтные куки при загрузке
        if (defaultHeaders != null && !defaultHeaders.cookies.isEmpty()) {
            cookiePane.setText(defaultHeaders.cookies);
        }

        JScrollPane cookieScrollPane = new JScrollPane(cookiePane);
        cookieScrollPane.setPreferredSize(new Dimension(0, 140));

        cookiePanel.add(cookieHeaderPanel, BorderLayout.NORTH);
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

        // Кнопка сохранения конфигурации
        JButton saveConfigButton = new JButton("Сохранить");
        saveConfigButton.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(saveConfigButton);

        // Добавляем contentPanel и buttonPanel в mainPanel
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Обработчик кнопки отправки
        sendButton.addActionListener((ActionEvent e) -> {
            String url = urlField.getText().trim();
            // Используем куки из поля, если они не пустые, иначе дефолтные
            String cookie = cookiePane.getText().trim();
            if (cookie.isEmpty() && defaultHeaders != null && !defaultHeaders.cookies.isEmpty()) {
                cookie = defaultHeaders.cookies;
            }

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

        // Обработчик кнопки сохранения конфигурации
        saveConfigButton.addActionListener(e -> {
            saveCurrentConfig();
            JOptionPane.showMessageDialog(this,
                    "Конфигурация сохранена",
                    "Сохранение",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // Обработчик кнопки истории
        historyButton.addActionListener(e -> showHistoryDialog());

        // Обработчик кнопки управления хедерами
        manageHeadersButton.addActionListener(e -> showHeadersDialog());

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

        // Обработчик закрытия окна - сохраняем конфигурацию
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveCurrentConfig();
            }
        });

        add(mainPanel);
        setVisible(true);
    }

    // Класс для хранения дефолтных хедеров
    private static class DefaultHeaders {
        String cookies = "";
        String headers = "";

        DefaultHeaders() {}

        DefaultHeaders(String cookies, String headers) {
            this.cookies = cookies != null ? cookies : "";
            this.headers = headers != null ? headers : "";
        }
    }

    // Класс для хранения конфигурации запроса (без куков в истории)
    private static class RequestConfig {
        String url;
        String requestBody;
        boolean isPost;
        String timestamp;

        RequestConfig(String url, String requestBody, boolean isPost) {
            this.url = url;
            this.requestBody = requestBody;
            this.isPost = isPost;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }
    }

    // Загрузка дефолтных хедеров из файла
    private void loadDefaultHeaders() {
        File headersFile = new File(HEADERS_FILE);
        if (headersFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(HEADERS_FILE)));
                Gson gson = new Gson();
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

                defaultHeaders = new DefaultHeaders(
                        jsonObject.has("cookies") ? jsonObject.get("cookies").getAsString() : "",
                        jsonObject.has("headers") ? jsonObject.get("headers").getAsString() : ""
                );

                System.out.println("Загружены дефолтные хедеры");
            } catch (Exception e) {
                System.out.println("Ошибка загрузки хедеров: " + e.getMessage());
                defaultHeaders = new DefaultHeaders();
            }
        } else {
            defaultHeaders = new DefaultHeaders();
        }
    }

    // Сохранение дефолтных хедеров в файл
    private void saveDefaultHeaders() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("cookies", defaultHeaders.cookies);
            jsonObject.addProperty("headers", defaultHeaders.headers);

            Files.write(Paths.get(HEADERS_FILE), gson.toJson(jsonObject).getBytes());
            System.out.println("Дефолтные хедеры сохранены");
        } catch (Exception e) {
            System.out.println("Ошибка сохранения хедеров: " + e.getMessage());
        }
    }

    // Загрузка конфигурации из файла
    private void loadConfig() {
        System.out.println(CONFIG_FILE);
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                Gson gson = new Gson();
                JsonArray jsonArray = JsonParser.parseString(content).getAsJsonArray();

                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    RequestConfig config = new RequestConfig(
                            obj.get("url").getAsString(),
                            obj.get("requestBody").getAsString(),
                            obj.get("isPost").getAsBoolean()
                    );
                    requestHistory.add(config);
                }
            } catch (Exception e) {
                System.out.println("Ошибка загрузки конфигурации: " + e.getMessage());
            }
        }
    }

    // Сохранение конфигурации в файл
    private void saveConfig() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonArray jsonArray = new JsonArray();

            for (RequestConfig config : requestHistory) {
                JsonObject obj = new JsonObject();
                obj.addProperty("url", config.url);
                obj.addProperty("requestBody", config.requestBody);
                obj.addProperty("isPost", config.isPost);
                obj.addProperty("timestamp", config.timestamp);
                jsonArray.add(obj);
            }

            Files.write(Paths.get(CONFIG_FILE), gson.toJson(jsonArray).getBytes());
        } catch (Exception e) {
            System.out.println("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }

    // Сохранение текущей конфигурации (без куков)
    private void saveCurrentConfig() {
        String url = urlField.getText().trim();
        String requestBody = rqPane.getText().trim();
        boolean isPost = postCheckBox.isSelected();

        if (!url.isEmpty()) {
            // Удаляем старые записи с таким же URL
            requestHistory.removeIf(config -> config.url.equals(url));

            // Добавляем новую запись в начало
            requestHistory.add(0, new RequestConfig(url, requestBody, isPost));

            // Ограничиваем размер истории
            if (requestHistory.size() > MAX_HISTORY) {
                requestHistory = requestHistory.subList(0, MAX_HISTORY);
            }

            saveConfig();
        }
    }

    // Диалог управления хедерами
    private void showHeadersDialog() {
        JDialog headersDialog = new JDialog(this, "Управление хедерами по умолчанию", true);
        headersDialog.setLayout(new BorderLayout());
        headersDialog.setSize(500, 400);
        headersDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Поле для куков
        JPanel cookiesPanel = new JPanel(new BorderLayout());
        cookiesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        JLabel cookiesLabel = new JLabel("Куки по умолчанию:");
        JTextArea cookiesArea = new JTextArea(3, 40);
        cookiesArea.setLineWrap(true);
        cookiesArea.setWrapStyleWord(true);
        cookiesArea.setText(defaultHeaders.cookies);
        JScrollPane cookiesScroll = new JScrollPane(cookiesArea);

        cookiesPanel.add(cookiesLabel, BorderLayout.NORTH);
        cookiesPanel.add(cookiesScroll, BorderLayout.CENTER);

        // Поле для других хедеров
        JPanel headersPanel = new JPanel(new BorderLayout());
        headersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        JLabel headersLabel = new JLabel("Другие заголовки (каждый с новой строки):");
        JTextArea headersArea = new JTextArea(3, 40);
        headersArea.setLineWrap(true);
        headersArea.setWrapStyleWord(true);
        headersArea.setText(defaultHeaders.headers);
        JScrollPane headersScroll = new JScrollPane(headersArea);

        headersPanel.add(headersLabel, BorderLayout.NORTH);
        headersPanel.add(headersScroll, BorderLayout.CENTER);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(cookiesPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(headersPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);

        headersDialog.add(mainPanel, BorderLayout.CENTER);

        // Обработчики кнопок
        saveButton.addActionListener(e -> {
            defaultHeaders.cookies = cookiesArea.getText().trim();
            defaultHeaders.headers = headersArea.getText().trim();
            saveDefaultHeaders();

            // Обновляем поле куков, если оно пустое
            if (cookiePane.getText().trim().isEmpty()) {
                cookiePane.setText(defaultHeaders.cookies);
            }

            headersDialog.dispose();
            JOptionPane.showMessageDialog(this, "Хедеры сохранены");
        });

        cancelButton.addActionListener(e -> headersDialog.dispose());

        headersDialog.setVisible(true);
    }

    // Показ диалога выбора из истории
    private void showHistoryDialog() {
        if (requestHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "История запросов пуста",
                    "История",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Создаем диалог с выбором
        JDialog historyDialog = new JDialog(this, "Выберите запрос из истории", true);
        historyDialog.setLayout(new BorderLayout());
        historyDialog.setSize(600, 400);
        historyDialog.setLocationRelativeTo(this);

        // Модель списка с отображением URL и времени
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (RequestConfig config : requestHistory) {
            String displayText = String.format("%s (%s)",
                    config.url,
                    config.timestamp.substring(0, 16).replace("T", " "));
            listModel.addElement(displayText);
        }

        JList<String> historyList = new JList<>(listModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(historyList);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loadButton = new JButton("Загрузить");
        JButton deleteButton = new JButton("Удалить");
        JButton cancelButton = new JButton("Отмена");

        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        historyDialog.add(listScrollPane, BorderLayout.CENTER);
        historyDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Обработчики кнопок
        loadButton.addActionListener(e -> {
            int selectedIndex = historyList.getSelectedIndex();
            if (selectedIndex >= 0) {
                RequestConfig config = requestHistory.get(selectedIndex);
                urlField.setText(config.url);
                rqPane.setText(config.requestBody);
                // Не загружаем куки из истории - используем дефолтные или текущие
                postCheckBox.setSelected(config.isPost);
                historyDialog.dispose();
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedIndex = historyList.getSelectedIndex();
            if (selectedIndex >= 0) {
                requestHistory.remove(selectedIndex);
                listModel.remove(selectedIndex);
                saveConfig();

                if (requestHistory.isEmpty()) {
                    historyDialog.dispose();
                }
            }
        });

        cancelButton.addActionListener(e -> historyDialog.dispose());

        historyDialog.setVisible(true);
    }

    // ... остальные методы (searchInTextPane, scrollToPosition, showSearchStatus, clearSearchHighlights) без изменений
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