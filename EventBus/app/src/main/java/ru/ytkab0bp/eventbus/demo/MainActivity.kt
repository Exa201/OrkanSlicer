package ru.ytkab0bp.eventbus.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.ytkab0bp.eventbus.EventBus
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.eventbus.demo.events.InheritedEvent
import ru.ytkab0bp.eventbus.demo.events.TestEvent

class MainActivity : AppCompatActivity() {
    @EventHandler(runOnMainThread = true)
    fun onTestEvent(event: TestEvent) {
        Toast.makeText(
            this,
            "Delivered event in ${System.currentTimeMillis() - event.startMs}ms!",
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!implementationRegistered) {
            EventBus.registerImpl(this)
            implementationRegistered = true
        }
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { EVENT_BUS.fireEvent(InheritedEvent()) }
        EVENT_BUS.registerListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EVENT_BUS.unregisterListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == R.id.action_settings) true else super.onOptionsItemSelected(item)

    companion object {
        private val EVENT_BUS = EventBus.newBus("main")
        private var implementationRegistered = false
    }
}
