import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessSchedulerGUI extends JFrame {
    private DefaultTableModel tableModel;
    private JTable table;
    private String headerLine;
    private JTextField processIdField, burstTimeField, arrivalTimeField, priorityField;
    private JTextField contextSwitchField, quantumField, rqSizeField;
    private String contextSwitch = "0", quantum = "20", rqSize = "3";

    public ProcessSchedulerGUI() {
        setTitle("Process Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 400);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(new Object[]{"Process ID", "Burst Time", "Arrival Time", "Priority"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton loadButton = new JButton("Load File");
        loadButton.addActionListener(e -> loadFile());

        JButton saveButton = new JButton("Save File");
        saveButton.addActionListener(e -> saveFile());

        JButton runButton = new JButton("Run Scheduler");
        runButton.addActionListener(e -> runScheduler());

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addRow());

        JButton setButton = new JButton("Set");
        setButton.addActionListener(e -> setInit());

        processIdField = new JTextField(5);
        burstTimeField = new JTextField(5);
        arrivalTimeField = new JTextField(5);
        priorityField = new JTextField(5);

        contextSwitchField = new JTextField(5);
        quantumField = new JTextField(5);
        rqSizeField = new JTextField(5);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.add(new JLabel("Process ID:"));
        inputPanel.add(processIdField);
        inputPanel.add(new JLabel("Burst Time:"));
        inputPanel.add(burstTimeField);
        inputPanel.add(new JLabel("Arrival Time:"));
        inputPanel.add(arrivalTimeField);
        inputPanel.add(new JLabel("Priority:"));
        inputPanel.add(priorityField);
        inputPanel.add(addButton);

        JPanel initPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; gbc.gridy = 0;
        initPanel.add(new JLabel("CONTEXT_SWITCH: "), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        initPanel.add(contextSwitchField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        initPanel.add(new JLabel("QUANTUM: "), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        initPanel.add(quantumField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        initPanel.add(new JLabel("RQ_SIZE: "), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        initPanel.add(rqSizeField, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        initPanel.add(setButton, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(runButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.NORTH);
        add(initPanel, BorderLayout.WEST);
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File loadedFile = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(loadedFile))) {
                headerLine = br.readLine();
                String line;
                tableModel.setRowCount(0);
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(" ");
                    tableModel.addRow(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(headerLine);
                bw.newLine();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        bw.write(Objects.requireNonNull(tableModel.getValueAt(i, j)).toString() + " ");
                    }
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setInit() {
        contextSwitch = contextSwitchField.getText().isEmpty() ? "0" : contextSwitchField.getText();
        quantum = quantumField.getText().isEmpty() ? "20" : quantumField.getText();
        rqSize = rqSizeField.getText().isEmpty() ? "3" : rqSizeField.getText();
    }

    private void addRow() {
        String processId = processIdField.getText();
        String burstTime = burstTimeField.getText();
        String arrivalTime = arrivalTimeField.getText();
        String priority = priorityField.getText();

        if (!processId.isEmpty() && !burstTime.isEmpty() && !arrivalTime.isEmpty() && !priority.isEmpty()) {
            tableModel.addRow(new Object[]{processId, burstTime, arrivalTime, priority});
            processIdField.setText("");
            burstTimeField.setText("");
            arrivalTimeField.setText("");
            priorityField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "모든 필드를 채워주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runScheduler() {
        JFrame newFrame = new JFrame("Choose Priority");
        newFrame.setSize(300, 200);
        newFrame.setLayout(new GridLayout(1, 4));

        String[] buttonNames = {"fcfs", "sjf", "srtf", "rr"};

        for (int i = 0; i < 4; i++) {
            JButton button = new JButton(buttonNames[i]);
            int finalI = i + 1;  // executeCommand에 전달될 값 조정
            button.addActionListener(e -> {
                executeCommand(finalI);
                newFrame.dispose();
            });
            newFrame.add(button);
        }

        newFrame.setLocationRelativeTo(this);
        newFrame.setVisible(true);
    }

    private void executeCommand(int priority) {
        new Thread(() -> {
            try {
                // 테이블 데이터를 임시 파일에 저장
                File tempFile = File.createTempFile("process_data", ".txt");
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        for (int j = 0; j < tableModel.getColumnCount(); j++) {
                            bw.write(Objects.requireNonNull(tableModel.getValueAt(i, j)).toString() + " ");
                        }
                        bw.newLine();
                    }
                }

                // 현재 작업 디렉토리의 경로를 얻습니다.
                String currentWorkingDirectory = System.getProperty("user.dir");
                // make clean
//                ProcessBuilder makeProcessBuilder2 = new ProcessBuilder("make", "clean");
//                makeProcessBuilder2.inheritIO();
//                Process makeProcess2 = makeProcessBuilder2.start();
//                int makeExitCode2 = makeProcess2.waitFor();
//                if (makeExitCode2 != 0) {
//                    System.out.println("Make Failed : " + makeExitCode2);
//                    return;
//                }

                ProcessBuilder makeProcessBuilder = new ProcessBuilder("make", "scheduler",
                        "CONTEXT_SWITCH=" + contextSwitch,
                        "QUANTUM=" + quantum,
                        "RQ_SIZE=" + rqSize);
                makeProcessBuilder.inheritIO();
                Process makeProcess = makeProcessBuilder.start();
                int makeExitCode = makeProcess.waitFor();
                if (makeExitCode != 0) {
                    System.out.println("Make Failed : " + makeExitCode);
                    return;
                }

                // String currentWorkingDirectory = System.getProperty("user.dir");
                // String command = "./bins/scheduler " + tempFile.getAbsolutePath() + " " + priority;

                // ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
                // runProcessBuilder.inheritIO();
                // Process runProcess = runProcessBuilder.start();
                // int runExitCode = runProcess.waitFor();
                // if (runExitCode != 0) {
                //     System.out.println("Run Failed : " + runExitCode);
                //     return;
                // }

                // 현재 작업 디렉토리의 경로를 얻습니다.
                // String currentWorkingDirectory = System.getProperty("user.dir");
                String command = "./bins/scheduler " + tempFile.getAbsolutePath() + " " + priority;

                ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
                // ProcessBuilder의 작업 디렉토리를 현재 작업 디렉토리로 설정합니다.
                processBuilder.directory(new File(currentWorkingDirectory));
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    SwingUtilities.invokeLater(() -> {
                        output.append(finalLine).append("\n");
                    });
                }
                // 프로세스가 종료될 때까지 대기
                int exitVal = process.waitFor();
                SwingUtilities.invokeLater(() -> {
                    if (exitVal == 0) {
                        System.out.println("실행 성공.");
                    } else {
                        // 오류 처리
                        System.out.println("실행 실패.");
                    }
                    showOutput(output.toString());
                });

            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void showOutput(String output) {
        JFrame outputFrame = new JFrame("Scheduler Output");
        outputFrame.setSize(1000, 600);

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea textArea = new JTextArea(output);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        panel.add(scrollPane, BorderLayout.CENTER);

        String[] lines = output.split("\n");
        HashMap<Integer, ArrayList<ProcessInfo>> processMap = new HashMap<>();
        ArrayList<ResultInfo> result = new ArrayList<>();
        int maxTime = 0;

        Pattern pattern = Pattern.compile("(\\d+)s : (Process|Monitor :) (\\d+) is (working|waiting)");
        Pattern pattern2 = Pattern.compile("Average (Response|Waiting|Turnaround) Time: (\\d+\\.\\d+)");

        for(String line : lines) {
            Matcher matcher = pattern.matcher(line);
            Matcher matcher2 = pattern2.matcher(line);
            if (matcher.find()) {
                int time = Integer.parseInt(matcher.group(1));
                int processId = Integer.parseInt(matcher.group(3));
                String action = matcher.group(4);

                processMap.putIfAbsent(processId, new ArrayList<>());
                processMap.get(processId).add(new ProcessInfo(time, action));
                if (time > maxTime) maxTime = time;
            }
            if (matcher2.find()) {
                double time2 = Double.parseDouble((matcher2.group(2)));
                result.add(new ResultInfo(time2));
                System.out.println("time2 = " + time2);
            }
        }

        // 결과 출력
        processMap.forEach((id, list) -> {
            System.out.println("Process " + id + ":");
            list.forEach(processInfo -> System.out.println(processInfo.toString()));
            System.out.println();
        });

        GanttChart ganttChart = new GanttChart(processMap, maxTime, result);
        JScrollPane chartScrollPane = new JScrollPane(ganttChart, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);


        panel.add(chartScrollPane, BorderLayout.SOUTH);
        outputFrame.add(panel);
        outputFrame.setVisible(true);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProcessSchedulerGUI gui = new ProcessSchedulerGUI();
            gui.setVisible(true);
        });
    }
}
