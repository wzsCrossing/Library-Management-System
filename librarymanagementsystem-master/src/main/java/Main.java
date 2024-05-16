import entities.Book;
import entities.Borrow;
import entities.Card;
import entities.Card.CardType;
import queries.*;
import queries.BorrowHistories.Item;
import queries.BookQueryConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import utils.ConnectConfig;
import utils.DatabaseConnector;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.alibaba.fastjson2.JSON;

public class Main {
    // main方法，代码从这开始
    public static void main(String[] args) throws IOException {
        // 创建HTTP服务器，监听指定端口
        // 这里是8000，建议不要80端口，容易和其他的撞
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // 添加handler，这里就绑定到/card路由
        // 所以localhost:8000/card是会有handler来处理
        server.createContext("/card", new CardHandler());
        server.createContext("/borrow", new BorrowHandler());
        server.createContext("/book", new BookHandler());
        // 启动服务器
        server.start();

        // 标识一下，这样才知道我的后端启动了（确信
        System.out.println("Server is listening on port 8000");
    }
    
    // 剩下的Main class...
    static class CardHandler implements HttpHandler {
        
        private DatabaseConnector connector;
        private LibraryManagementSystem library;

        private static ConnectConfig connectConfig = null;

        static {
            try {
                // parse connection config from "resources/application.yaml"
                connectConfig = new ConnectConfig();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        public static class CardItem {
            private int cardId;
            private String name;
            private String department;
            private String type;

            public CardItem(int cardId, String name, String department, String type) {
                this.cardId = cardId;
                this.name = name;
                this.department = department;
                this.type = type;
            }

            public int getCardId() {
                return cardId;
            }

            public String getName() {
                return name;
            }

            public String getDepartment() {
                return department;
            }

            public String getType() {
                return type;
            }
        }

        public CardHandler() {
            try {
                // connect to database
                connector = new DatabaseConnector(connectConfig);
                library = new LibraryManagementSystemImpl(connector);
                System.out.println("Successfully init class CardHandler.");
                boolean connStatus = connector.connect();
                if (connStatus) {
                    System.out.println("Successfully connect to database.");
                } else {
                    System.out.println("Failed to connect to database.");
                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("PUT")) {
                // 处理PUT
                handlePutRequest(exchange);
            } else if (requestMethod.equals("DELETE")) {
                // 处理DELETE
                handleDeleteRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            ApiResult QueryResult = library.showCards();

            if (QueryResult.ok) {
                CardList resCardList = (CardList) QueryResult.payload;
                List<Card> cards = resCardList.getCards();
                List<CardItem> items = new ArrayList<>();

                for (int i = 0; i < resCardList.getCount(); ++i) {
                    int cardId = cards.get(i).getCardId();
                    String name = cards.get(i).getName();
                    String department = cards.get(i).getDepartment();
                    String type = (cards.get(i).getType().getStr().equals("S")) ? "学生" : "教师";

                    items.add(new CardItem(cardId, name, department, type));
                }

                String response = JSON.toJSONString(items);

                // 写
                outputStream.write(response.getBytes());
            }
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

            String request = requestBodyBuilder.toString();
            String[] param = request.split("[\"]");

            ApiResult result = library.registerCard(new Card(0, param[3], param[7], param[11].equals("学生") ? CardType.Student : CardType.Teacher));
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok) {
                outputStream.write("Success".getBytes());
            } else {
                outputStream.write("Fail".getBytes());
            }
            
            outputStream.close();
        }

        private void handlePutRequest(HttpExchange exchange) throws IOException {
            // 读取PUT请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received PUT request to modify card with data: " + requestBodyBuilder.toString());

            String request = requestBodyBuilder.toString();
            CardItem item = JSON.parseObject(request, CardItem.class);

            int cardId = item.getCardId();
            String name = item.getName();
            String department = item.getDepartment();
            String type = item.getType().equals("学生") ? "S" : "T";
            ApiResult result = library.modifyCardInfo(new Card(cardId, name, department, CardType.values(type)));
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            if (result.ok) {
                outputStream.write("Success".getBytes());
            } else {
                outputStream.write("Fail".getBytes());
            }
            outputStream.close();
        }

        private void handleDeleteRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            String[] param = query.split("[=]");

            ApiResult result = library.removeCard(Integer.parseInt(param[1]));
            if (result.ok) {
                outputStream.write("Success".getBytes());
            } else {
                outputStream.write("Fail".getBytes());
            }
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码204
            exchange.sendResponseHeaders(204, -1);
        }
    }
    
    static class BorrowHandler implements HttpHandler {
        
        private DatabaseConnector connector;
        private LibraryManagementSystem library;

        private static ConnectConfig connectConfig = null;

        public static class BorrowItem {
            private int cardID;
            private int bookID;
            private String borrowTime;
            private String returnTime;

            public BorrowItem(int cardID, int bookID, String borrowTime, String returnTime) {
                this.cardID = cardID;
                this.bookID = bookID;
                this.borrowTime = borrowTime;
                this.returnTime = returnTime;
            }

            public int getCardID() {
                return cardID;
            }
    
            public int getBookID() {
                return bookID;
            }

            public String getBorrowTime() {
                return borrowTime;
            }
    
            public String getReturnTime() {
                return returnTime;
            }
        }

        static {
            try {
                // parse connection config from "resources/application.yaml"
                connectConfig = new ConnectConfig();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        public BorrowHandler() {
            try {
                // connect to database
                connector = new DatabaseConnector(connectConfig);
                library = new LibraryManagementSystemImpl(connector);
                System.out.println("Successfully init class BorrowHandler.");
                boolean connStatus = connector.connect();
                if (connStatus) {
                    System.out.println("Successfully connect to database.");
                } else {
                    System.out.println("Failed to connect to database.");
                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            String[] param = query.split("[=]");
            
            if (param.length > 1) {
                ApiResult QueryResult = library.showBorrowHistory(Integer.parseInt(param[1]));

                if (QueryResult.ok) {
                    BorrowHistories resCardList = (BorrowHistories) QueryResult.payload;
                    List<Item> items = resCardList.getItems();
                    List<BorrowItem> borrowItems = new ArrayList<>();

                    for (int i = 0; i < resCardList.getCount(); ++i) {
                        int bookID = items.get(i).getBookId();
                        int cardId = items.get(i).getCardId();

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                        Date date = new Date(items.get(i).getBorrowTime());
                        String borrowTime = sdf.format(date);

                        long rTime = items.get(i).getReturnTime();
                        String returnTime;
                        if (rTime == 0L) {
                            returnTime = "/";
                        } else {
                            date = new Date(rTime);
                            returnTime = sdf.format(date);
                        }

                        borrowItems.add(new BorrowItem(cardId, bookID, borrowTime, returnTime));
                    }

                    String response = JSON.toJSONString(borrowItems);
                    // 写
                    outputStream.write(response.getBytes());
                }
            }
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码204
            exchange.sendResponseHeaders(204, -1);
        }
    }

    static class BookHandler implements HttpHandler {
        
        private DatabaseConnector connector;
        private LibraryManagementSystem library;

        private static ConnectConfig connectConfig = null;

        static {
            try {
                // parse connection config from "resources/application.yaml"
                connectConfig = new ConnectConfig();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        public BookHandler() {
            try {
                // connect to database
                connector = new DatabaseConnector(connectConfig);
                library = new LibraryManagementSystemImpl(connector);
                System.out.println("Successfully init class BookHandler.");
                boolean connStatus = connector.connect();
                if (connStatus) {
                    System.out.println("Successfully connect to database.");
                } else {
                    System.out.println("Failed to connect to database.");
                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("PUT")) {
                // 处理PUT
                handlePutRequest(exchange);
            } else if (requestMethod.equals("DELETE")) {
                // 处理DELETE
                handleDeleteRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            ApiResult QueryResult = library.queryBook(new BookQueryConditions());

            if (QueryResult.ok) {
                BookQueryResults resBookList = (BookQueryResults) QueryResult.payload;

                String response = JSON.toJSONString(resBookList.getResults());

                // 写
                outputStream.write(response.getBytes());
            }
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received POST request: " + requestBodyBuilder.toString());

            String request = requestBodyBuilder.toString();
            String[] param = request.split("[{,:\"}]");
            String message = "";

            if (param[5].equals("store")) {
                String title = param[11];
                String category = param[17];
                String press = param[23];
                int publishYear = Integer.parseInt(param[28]);
                String author = param[33];
                double price = Double.parseDouble(param[38]);
                int stock = Integer.parseInt(param[42]);

                Book book = new Book(category, title, press, publishYear, author, price, stock);

                ApiResult result = library.storeBook(book);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = "Fail";
                }
            } else {
                param = request.split("[{}]");
                List<Book> books = new ArrayList<>();

                for (int i = 2; i < param.length; i += 2) {
                    String dataString = "{" + param[i].replace("\\", "") + "}";
                    Book item = JSON.parseObject(dataString, Book.class);
                    books.add(item);
                }

                ApiResult result = library.storeBook(books);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = "Fail";
                }
            }

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();
        }

        private void handlePutRequest(HttpExchange exchange) throws IOException {
            // 读取PUT请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received PUT request: " + requestBodyBuilder.toString());

            String request = requestBodyBuilder.toString();
            String[] param = request.split("[{,:\"}]");
            String message = "";

            if (param[5].equals("borrow")) {
                Borrow borrow = new Borrow(Integer.parseInt(param[10]), Integer.parseInt(param[15]));
                borrow.resetBorrowTime();
                ApiResult result = library.borrowBook(borrow);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = result.message;
                }
            } else if (param[5].equals("return")) {
                Borrow borrow = new Borrow(Integer.parseInt(param[10]), Integer.parseInt(param[15]));
                borrow.resetReturnTime();
                ApiResult result = library.returnBook(borrow);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = result.message;
                }
            } else if (param[5].equals("info")) {
                int bookId = Integer.parseInt(param[10]);
                String category = param[15];
                String title = param[21];
                String press = param[27];
                int publishYear = Integer.parseInt(param[32]);
                String author = param[37];
                double price = Double.parseDouble(param[42]);

                Book book = new Book(category, title, press, publishYear, author, price, 0);
                book.setBookId(bookId);

                ApiResult result = library.modifyBookInfo(book);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = "Fail";
                }
            } else if (param[5].equals("stock")) {
                int bookId = Integer.parseInt(param[10]);
                int deltaStock = Integer.parseInt(param[14]);

                ApiResult result = library.incBookStock(bookId, deltaStock);
                if (result.ok) {
                    message = "Success";
                } else {
                    message = "Fail";
                }
            }

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(message.getBytes());
            outputStream.close();
        }

        private void handleDeleteRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            String[] param = query.split("[=]");

            ApiResult result = library.removeBook(Integer.parseInt(param[1]));
            if (result.ok) {
                outputStream.write("Success".getBytes());
            } else {
                outputStream.write("Fail".getBytes());
            }
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码204
            exchange.sendResponseHeaders(204, -1);
        }
    }
}