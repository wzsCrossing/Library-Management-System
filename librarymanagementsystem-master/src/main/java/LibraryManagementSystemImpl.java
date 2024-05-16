import entities.Book;
import entities.Borrow;
import entities.Card;
import entities.Card.CardType;
import queries.*;
import queries.BorrowHistories.Item;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.naming.spi.DirStateFactory.Result;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            String sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            preparedStatement.setString(1, book.getCategory());
            preparedStatement.setString(2, book.getTitle());
            preparedStatement.setString(3, book.getPress());
            preparedStatement.setInt(4, book.getPublishYear());
            preparedStatement.setString(5, book.getAuthor());
            preparedStatement.setDouble(6, book.getPrice());
            preparedStatement.setInt(7, book.getStock());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setBookId(generatedKeys.getInt(1));
                    }
                }
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            String sqlUpdate = "UPDATE book SET stock = stock + ? WHERE book_id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sqlUpdate);
            preparedStatement.setInt(1, deltaStock);
            preparedStatement.setInt(2, bookId);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("invalid book id.");
            } else {
                String sqlSelect = "SELECT stock FROM book WHERE book_id = ?";
                preparedStatement = conn.prepareStatement(sqlSelect);
                preparedStatement.setInt(1, bookId);

                ResultSet rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    Integer stock = rs.getInt("stock");
                    if (stock < 0) {
                        throw new Exception("invalid book stock.");
                    }
                }
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            String sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int nOps = books.size();
            for (int i = 0; i < nOps; ++i) {
                Book book = books.get(i);

                preparedStatement.setString(1, book.getCategory());
                preparedStatement.setString(2, book.getTitle());
                preparedStatement.setString(3, book.getPress());
                preparedStatement.setInt(4, book.getPublishYear());
                preparedStatement.setString(5, book.getAuthor());
                preparedStatement.setDouble(6, book.getPrice());
                preparedStatement.setInt(7, book.getStock());

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                for (int i = 0; i < nOps; ++i) {
                    if (generatedKeys.next()) {
                        books.get(i).setBookId(generatedKeys.getInt(1));
                    }
                }
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            String sqlSelect = "SELECT count(*) FROM borrow WHERE book_id = ? AND return_time = 0";
            PreparedStatement preparedStatement = conn.prepareStatement(sqlSelect);

            preparedStatement.setInt(1, bookId);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    throw new Exception("Someone has not returned the book.");
                }
            }

            String sqlDelete = "DELETE FROM book WHERE book_id = ?";
            preparedStatement = conn.prepareStatement(sqlDelete);

            preparedStatement.setInt(1, bookId);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("Non-exist book.");
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            String sql = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setString(1, book.getCategory());
            preparedStatement.setString(2, book.getTitle());
            preparedStatement.setString(3, book.getPress());
            preparedStatement.setInt(4, book.getPublishYear());
            preparedStatement.setString(5, book.getAuthor());
            preparedStatement.setDouble(6, book.getPrice());
            preparedStatement.setInt(7, book.getBookId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("Non-exist book.");
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        List<Book> results = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM book");
            List<Object> parameters = new ArrayList<>();

            boolean andAnd = false;

            if (conditions.getCategory() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("category = ?");
                parameters.add(conditions.getCategory());
            }
            if (conditions.getTitle() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("title LIKE ?");
                parameters.add("%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("press LIKE ?");
                parameters.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("publish_year >= ?");
                parameters.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("publish_year <= ?");
                parameters.add(conditions.getMaxPublishYear());
            }
            if (conditions.getAuthor() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("author LIKE ?");
                parameters.add("%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("price >= ?");
                parameters.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                if (!andAnd) {
                    sql.append(" WHERE ");
                    andAnd = true;
                } else sql.append(" AND ");
                
                sql.append("price <= ?");
                parameters.add(conditions.getMaxPrice());
            }

            sql.append(" ORDER BY " + conditions.getSortBy().getValue() + " " + conditions.getSortOrder().getValue() + ", book_id ASC");

            PreparedStatement preparedStatement = conn.prepareStatement(sql.toString());

            for (int i = 0; i < parameters.size(); ++i) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }

            // System.err.println(preparedStatement.toString());

            ResultSet rs = preparedStatement.executeQuery();

            Integer i = 0;
            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                String category = rs.getString("category");
                String title = rs.getString("title");
                String press = rs.getString("press");
                int publishYear = rs.getInt("publish_year");
                String author = rs.getString("author");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                results.add(new Book(category, title, press, publishYear, author, price, stock));
                results.get(i).setBookId(bookId);

                i = i + 1;
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null, new queries.BookQueryResults(results));
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            String sql1 = "SELECT * FROM card WHERE card_id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql1);

            preparedStatement.setInt(1, borrow.getCardId());
            
            ResultSet rs = preparedStatement.executeQuery();
            
            if (!rs.next()) {
                throw new Exception("Non-exist card.");
            }

            String sql2 = "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0";
            preparedStatement = conn.prepareStatement(sql2);

            preparedStatement.setInt(1, borrow.getBookId());
            preparedStatement.setInt(2, borrow.getCardId());
            
            rs = preparedStatement.executeQuery();
            
            if (rs.next()) {
                throw new Exception("The user has not returned the book.");
            }

            if (!(incBookStock(borrow.getBookId(), -1).ok)) {
                throw new Exception("incBookStock error.");
            }

            String sqlInsert = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)";
            preparedStatement = conn.prepareStatement(sqlInsert);

            preparedStatement.setInt(1, borrow.getCardId());
            preparedStatement.setInt(2, borrow.getBookId());
            preparedStatement.setLong(3, borrow.getBorrowTime());

            preparedStatement.executeUpdate();

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            String sql = "SELECT * FROM card WHERE card_id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setInt(1, borrow.getCardId());

            ResultSet rs = preparedStatement.executeQuery();

            if (!rs.next()) {
                throw new Exception("Non-exist card.");
            }

            sql = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND return_time = 0 AND borrow_time < ?";
            preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setLong(1, borrow.getReturnTime());
            preparedStatement.setInt(2, borrow.getCardId());
            preparedStatement.setInt(3, borrow.getBookId());
            preparedStatement.setLong(4, borrow.getReturnTime());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("Failed to return the book.");
            } else {
                if (!(incBookStock(borrow.getBookId(), 1).ok)) {
                    throw new Exception("incBookStock error.");
                }
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        List<Item> items = new ArrayList<>();
        try {
            String sql = "SELECT * FROM borrow, book WHERE card_id = ? AND borrow.book_id = book.book_id ORDER BY borrow_time DESC, book.book_id ASC";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setInt(1, cardId);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                int bookId = rs.getInt("book.book_id");
                String category = rs.getString("category");
                String title = rs.getString("title");
                String press = rs.getString("press");
                int publishYear = rs.getInt("publish_year");
                String author = rs.getString("author");
                double price = rs.getDouble("price");
                long borrowTime = rs.getLong("borrow_time");
                long returnTime = rs.getLong("return_time");

                Item record = new Item();
                record.setCardId(cardId);
                record.setBookId(bookId);
                record.setCategory(category);
                record.setTitle(title);
                record.setPress(press);
                record.setPublishYear(publishYear);
                record.setAuthor(author);
                record.setPrice(price);
                record.setBorrowTime(borrowTime);
                record.setReturnTime(returnTime);

                items.add(record);
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null, new queries.BorrowHistories(items));
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            String sql = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            preparedStatement.setString(1, card.getName());
            preparedStatement.setString(2, card.getDepartment());
            preparedStatement.setString(3, card.getType().getStr());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        card.setCardId(generatedKeys.getInt(1));
                    }
                }
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            String sqlSelect = "SELECT count(*) FROM borrow WHERE card_id = ? AND return_time = 0";
            PreparedStatement preparedStatement = conn.prepareStatement(sqlSelect);

            preparedStatement.setInt(1, cardId);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    throw new Exception("There exists un-returned books under this user.");
                }
            }

            String sqlDelete = "DELETE FROM card WHERE card_id = ?";
            preparedStatement = conn.prepareStatement(sqlDelete);

            preparedStatement.setInt(1, cardId);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("Non-exist card.");
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            String sql = "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setString(1, card.getName());
            preparedStatement.setString(2, card.getDepartment());
            preparedStatement.setString(3, card.getType().getStr());
            preparedStatement.setInt(4, card.getCardId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("Non-exist card.");
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    
    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        List<Card> cards = new ArrayList<>();
        try {
            String sql = "SELECT * FROM card ORDER BY card_id";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                int cardId = rs.getInt("card_id");
                String name = rs.getString("name");
                String department = rs.getString("department");
                CardType type = Card.CardType.values(rs.getString("type"));

                cards.add(new Card(cardId, name, department, type));
            }

            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null, new queries.CardList(cards));
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
