package com.example.caffocus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.caffocus.databinding.ActivityCalendaractivityBinding
import com.example.caffocus.databinding.ActivityMainBinding
import com.example.caffocus.databinding.ActivityTimerBinding
import com.example.caffocus.databinding.ActivityUseractivityBinding
import com.example.caffocus.databinding.ItemTodoBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

data class TodoItem(
    val content: String,
    val timestamp: Date = Date(),
    val date: Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
)

class TodoAdapter(private val allTodoItems: MutableList<TodoItem>) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val displayedTodoItems: MutableList<TodoItem> = mutableListOf()

    init {
        displayedTodoItems.addAll(allTodoItems)
    }

    class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = displayedTodoItems[position]
        holder.binding.todoContent.text = item.content

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.binding.todoTimestamp.text = dateFormat.format(item.timestamp)
    }

    override fun getItemCount() = displayedTodoItems.size

    fun addTodo(todo: TodoItem) {
        allTodoItems.add(todo)

        if (shouldDisplayTodo(todo)) {
            displayedTodoItems.add(todo)
            notifyItemInserted(displayedTodoItems.size - 1)
        }
    }

    fun filterByDate(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val startOfNextDay = calendar.time

        displayedTodoItems.clear()

        for (todo in allTodoItems) {
            if (shouldDisplayTodo(todo, startOfDay, startOfNextDay)) {
                displayedTodoItems.add(todo)
            }
        }

        notifyDataSetChanged()
    }

    private fun shouldDisplayTodo(todo: TodoItem, startOfDay: Date? = null, startOfNextDay: Date? = null): Boolean {
        if (startOfDay == null || startOfNextDay == null) return true

        return !todo.date.before(startOfDay) && todo.date.before(startOfNextDay)
    }
}

class calendaractivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendaractivityBinding
    private lateinit var todoAdapter: TodoAdapter
    private val todoItems = mutableListOf<TodoItem>()
    private var selectedDate: Date = Calendar.getInstance().time
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    
    companion object {
        const val PREF_NAME = "TodoPreferences"
        const val TODO_ITEMS_KEY = "todo_items"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCalendaractivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        loadTodoItems()

        binding.timer2.setOnClickListener {
            intent = Intent(this, timer::class.java)
            startActivity(intent)

        }

        binding.user2.setOnClickListener {
            intent = Intent(this, useractivity::class.java)
            startActivity(intent)
        }



        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.addTodoButton.apply {
            isClickable = true
            isFocusable = true
            background = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT).let {
                android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33000000), null, null)
            }
        }

        setupCalendar()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time

            todoAdapter.filterByDate(selectedDate)

            val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
            binding.todoHeaderText.text = "${dateFormat.format(selectedDate)} TODO"
        }
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(todoItems)
        binding.todoRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.todoRecyclerView.adapter = todoAdapter

        todoAdapter.filterByDate(selectedDate)
    }

    private fun setupListeners() {
        binding.addTodoButton.setOnClickListener {
            binding.todoInputLayout.visibility = View.VISIBLE
            binding.todoInput.requestFocus()
        }

        binding.saveButton.setOnClickListener {
            val content = binding.todoInput.text.toString().trim()
            if (content.isNotEmpty()) {
                val todoItem = TodoItem(
                    content = content,
                    timestamp = Date(),
                    date = selectedDate
                )
                todoAdapter.addTodo(todoItem)

                saveTodoItems()

                binding.todoInput.text.clear()
                binding.todoInputLayout.visibility = View.GONE

                binding.todoRecyclerView.smoothScrollToPosition(todoAdapter.itemCount - 1)
            }
        }

        binding.cancelButton.setOnClickListener {
            binding.todoInput.text.clear()
            binding.todoInputLayout.visibility = View.GONE
        }
    }

    private fun saveTodoItems() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(todoItems)
        editor.putString(TODO_ITEMS_KEY, json)
        editor.apply()
    }

    private fun loadTodoItems() {
        val json = sharedPreferences.getString(TODO_ITEMS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<TodoItem>>() {}.type
            val loadedItems: List<TodoItem> = gson.fromJson(json, type)
            todoItems.clear()
            todoItems.addAll(loadedItems)
        }
    }
}