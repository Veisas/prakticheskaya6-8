import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private TextView selectedNoteTextView;
    private Button deleteButton, addButton, aboutButton, exitButton;
    private SQLiteDatabase database;
    private List<String> notes = new ArrayList<>();
    private int selectedNotePosition = -1;
    private boolean isAboutShown = false;
    private boolean isNotepadShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация базы данных
        SQLiteOpenHelper dbHelper = new SQLiteOpenHelper(this, "notepad.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, date DATETIME DEFAULT CURRENT_TIMESTAMP)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS notes");
                onCreate(db);
            }
        };
        database = dbHelper.getWritableDatabase();

        // Инициализация элементов интерфейса
        listView = findViewById(R.id.listView);
        selectedNoteTextView = findViewById(R.id.selectedNoteTextView);
        deleteButton = findViewById(R.id.deleteButton);
        addButton = findViewById(R.id.addButton);
        aboutButton = findViewById(R.id.aboutButton);
        exitButton = findViewById(R.id.exitButton);

        // Загрузка заметок
        loadNotes();

        // Обработчики событий
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedNotePosition = position;
                showNoteContent(position);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedNotePosition != -1) {
                    deleteNote(selectedNotePosition);
                } else {
                    Toast.makeText(MainActivity.this, "Выберите запись для удаления", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotepad();
            }
        });

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAbout();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadNotes() {
        notes.clear();
        Cursor cursor = database.rawQuery("SELECT id, title, date FROM notes ORDER BY date DESC", null);
        while (cursor.moveToNext()) {
            notes.add(cursor.getString(1) + " (" + cursor.getString(2) + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, notes);
        listView.setAdapter(adapter);
    }

    private void showNoteContent(int position) {
        Cursor cursor = database.rawQuery("SELECT content FROM notes ORDER BY date DESC LIMIT 1 OFFSET " + position, null);
        if (cursor.moveToFirst()) {
            selectedNoteTextView.setText(cursor.getString(0));
        }
        cursor.close();
    }

    private void deleteNote(int position) {
        Cursor cursor = database.rawQuery("SELECT id FROM notes ORDER BY date DESC LIMIT 1 OFFSET " + position, null);
        if (cursor.moveToFirst()) {
            database.delete("notes", "id = ?", new String[]{String.valueOf(cursor.getInt(0))});
            Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        
        selectedNotePosition = -1;
        selectedNoteTextView.setText("");
        loadNotes();
    }

    private void showNotepad() {
        // Скрываем основные элементы
        findViewById(R.id.mainLayout).setVisibility(View.GONE);
        
        // Создаем элементы для редактирования
        LinearLayout notepadLayout = new LinearLayout(this);
        notepadLayout.setOrientation(LinearLayout.VERTICAL);
        notepadLayout.setPadding(32, 32, 32, 32);
        
        TextView titleLabel = new TextView(this);
        titleLabel.setText("Название записи:");
        titleLabel.setTextSize(16);
        
        EditText titleEdit = new EditText(this);
        titleEdit.setHint("Введите название...");
        
        TextView contentLabel = new TextView(this);
        contentLabel.setText("Текст:");
        contentLabel.setTextSize(16);
        
        EditText contentEdit = new EditText(this);
        contentEdit.setHint("Введите текст...");
        contentEdit.setMinLines(5);
        
        Button saveButton = new Button(this);
        saveButton.setText("Сохранить");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEdit.getText().toString();
                String content = contentEdit.getText().toString();
                
                if (title.isEmpty() || content.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("content", content);
                database.insert("notes", null, values);
                
                Toast.makeText(MainActivity.this, "Запись сохранена", Toast.LENGTH_SHORT).show();
                hideNotepad();
                loadNotes();
            }
        });
        
        Button backButton = new Button(this);
        backButton.setText("Назад");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNotepad();
            }
        });
        
        // Добавляем элементы в layout
        notepadLayout.addView(titleLabel);
        notepadLayout.addView(titleEdit);
        notepadLayout.addView(contentLabel);
        notepadLayout.addView(contentEdit);
        notepadLayout.addView(saveButton);
        notepadLayout.addView(backButton);
        
        notepadLayout.setId(R.id.notepadLayout);
        ((LinearLayout)findViewById(R.id.container)).addView(notepadLayout);
        isNotepadShown = true;
    }

    private void hideNotepad() {
        ((LinearLayout)findViewById(R.id.container)).removeView(findViewById(R.id.notepadLayout));
        findViewById(R.id.mainLayout).setVisibility(View.VISIBLE);
        isNotepadShown = false;
    }

    private void showAbout() {
        // Скрываем основные элементы
        findViewById(R.id.mainLayout).setVisibility(View.GONE);
        
        // Создаем элементы "О программе"
        LinearLayout aboutLayout = new LinearLayout(this);
        aboutLayout.setOrientation(LinearLayout.VERTICAL);
        aboutLayout.setPadding(32, 32, 32, 32);
        aboutLayout.setGravity(LinearLayout.TEXT_ALIGNMENT_CENTER);
        
        TextView aboutText = new TextView(this);
        aboutText.setText("Электронная записная книжка\n\nАвтор: Ваше имя\nГруппа: Ваша группа");
        aboutText.setTextSize(18);
        aboutText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        Button backButton = new Button(this);
        backButton.setText("Назад");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAbout();
            }
        });
        
        // Добавляем элементы в layout
        aboutLayout.addView(aboutText);
        aboutLayout.addView(backButton);
        
        aboutLayout.setId(R.id.aboutLayout);
        ((LinearLayout)findViewById(R.id.container)).addView(aboutLayout);
        isAboutShown = true;
    }

    private void hideAbout() {
        ((LinearLayout)findViewById(R.id.container)).removeView(findViewById(R.id.aboutLayout));
        findViewById(R.id.mainLayout).setVisibility(View.VISIBLE);
        isAboutShown = false;
    }

    @Override
    public void onBackPressed() {
        if (isNotepadShown) {
            hideNotepad();
        } else if (isAboutShown) {
            hideAbout();
        } else {
            super.onBackPressed();
        }
    }
}