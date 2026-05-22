import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmokeQuestionSeeder {

    public static void main(String[] args) throws Exception {
        String jdbcUrl = getArg(args, "--jdbc-url");
        String user = getArg(args, "--user");
        String password = getArg(args, "--password");
        long questionId = Long.parseLong(getArg(args, "--question-id"));
        String title = getArg(args, "--title");
        int difficulty = Integer.parseInt(getArg(args, "--difficulty"));
        long timeLimit = Long.parseLong(getArg(args, "--time-limit"));
        long spaceLimit = Long.parseLong(getArg(args, "--space-limit"));
        String content = decodeBase64(getArg(args, "--content-base64"));
        String questionCase = decodeBase64(getArg(args, "--question-case-base64"));
        String defaultCode = decodeBase64(getArg(args, "--default-code-base64"));
        String mainFuc = decodeBase64(getArg(args, "--main-fuc-base64"));

        String replaceSql = """
                REPLACE INTO tb_question
                (question_id, title, difficulty, training_enabled, time_limit, space_limit, content, question_case, default_code, main_fuc, create_by, create_time)
                VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?, 1, NOW())
                """;

        String verifySql = """
                SELECT question_id, title, difficulty, time_limit, space_limit
                FROM tb_question
                WHERE question_id = ?
                """;

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            try (PreparedStatement statement = connection.prepareStatement(replaceSql)) {
                statement.setLong(1, questionId);
                statement.setString(2, title);
                statement.setInt(3, difficulty);
                statement.setLong(4, timeLimit);
                statement.setLong(5, spaceLimit);
                statement.setString(6, content);
                statement.setString(7, questionCase);
                statement.setString(8, defaultCode);
                statement.setString(9, mainFuc);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(verifySql)) {
                statement.setLong(1, questionId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("Smoke question verification failed");
                    }
                    System.out.println("SEEDED_QUESTION_ID=" + resultSet.getLong("question_id"));
                    System.out.println("SEEDED_TITLE=" + resultSet.getString("title"));
                    System.out.println("SEEDED_DIFFICULTY=" + resultSet.getInt("difficulty"));
                    System.out.println("SEEDED_TIME_LIMIT=" + resultSet.getLong("time_limit"));
                    System.out.println("SEEDED_SPACE_LIMIT=" + resultSet.getLong("space_limit"));
                }
            }
        }
    }

    private static String getArg(String[] args, String name) {
        for (int index = 0; index < args.length - 1; index++) {
            if (name.equals(args[index])) {
                return args[index + 1];
            }
        }
        throw new IllegalArgumentException("Missing argument: " + name);
    }

    private static String decodeBase64(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
