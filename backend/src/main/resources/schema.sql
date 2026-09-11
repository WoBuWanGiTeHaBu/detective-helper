PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS book (
                                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                    name        TEXT    NOT NULL,
                                    cover_type  TEXT    NOT NULL DEFAULT 'color',
                                    cover_value TEXT,
                                    cover_text  TEXT,
                                    sort_order  INTEGER NOT NULL DEFAULT 0,
                                    created_at  TEXT    NOT NULL,
                                    updated_at  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS event (
                                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                     book_id     INTEGER NOT NULL,
                                     name        TEXT    NOT NULL,
                                     sort_order  INTEGER NOT NULL DEFAULT 0,
                                     created_at  TEXT    NOT NULL,
                                     updated_at  TEXT    NOT NULL,
                                     FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS page (
                                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                    event_id    INTEGER NOT NULL,
                                    name        TEXT    NOT NULL,
                                    sort_order  INTEGER NOT NULL DEFAULT 0,
                                    canvas_data TEXT    NOT NULL DEFAULT '{}',
                                    created_at  TEXT    NOT NULL,
                                    updated_at  TEXT    NOT NULL,
                                    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS relation_graph (
                                              id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                              book_id     INTEGER NOT NULL,
                                              name        TEXT    NOT NULL,
                                              data        TEXT    NOT NULL DEFAULT '{}',
                                              created_at  TEXT    NOT NULL,
                                              updated_at  TEXT    NOT NULL,
                                              FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_event_book            ON event(book_id);
CREATE INDEX IF NOT EXISTS idx_page_event            ON page(event_id);
CREATE INDEX IF NOT EXISTS idx_relation_graph_book   ON relation_graph(book_id);
CREATE INDEX IF NOT EXISTS idx_book_sort             ON book(sort_order);
CREATE INDEX IF NOT EXISTS idx_event_sort            ON event(book_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_page_sort             ON page(event_id, sort_order);