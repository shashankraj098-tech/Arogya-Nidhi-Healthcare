package com.example.arogyanidhi

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var etIncome: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var spinnerCard: Spinner
    private lateinit var spinnerOcc: Spinner
    private lateinit var cbDisability: CheckBox
    private lateinit var btnCheck: Button
    private lateinit var tvEligible: TextView
    private lateinit var tvIneligible: TextView
    private lateinit var cardResult: CardView
    private lateinit var etSearchDistrict: EditText
    private lateinit var btnHospitals: Button
    private lateinit var layoutHelpfulTools: LinearLayout
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var progressBar: ProgressBar

    private val hospitalMap = mapOf(
        "Bangalore" to listOf("Apollo Hospital", "Manipal Hospital", "Victoria Hospital (Govt)"),
        "Mysore" to listOf("JSS Hospital", "Columbia Asia", "Narayana Hrudalaya", "K.R. Hospital (Govt)"),
        "Hampi" to listOf("VIMS Bellary", "Hampi Heritage Health Centre", "Govt Civil Hospital, Hospet")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSpinners()
        setupStepper()

        btnCheck.setOnClickListener {
            hideKeyboard()
            checkEligibility()
        }

        btnHospitals.setOnClickListener {
            hideKeyboard()
            searchHospitals()
        }

        findViewById<Button>(R.id.btnDocs).setOnClickListener { showDocumentGuide() }

        findViewById<Button>(R.id.btnPortal).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pmjay.gov.in/")))
        }

        findViewById<Button>(R.id.btnRestart).setOnClickListener { restartQuiz() }
        
        findViewById<Button>(R.id.btnHelpline).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:14555")))
        }
    }

    private fun initViews() {
        etIncome = findViewById(R.id.etIncome)
        etAge = findViewById(R.id.etAge)
        spinnerCard = findViewById(R.id.spinnerCard)
        spinnerOcc = findViewById(R.id.spinnerOccupation)
        cbDisability = findViewById(R.id.cbDisability)
        btnCheck = findViewById(R.id.btnCheck)
        tvEligible = findViewById(R.id.tvEligibleSchemes)
        tvIneligible = findViewById(R.id.tvIneligibleSchemes)
        cardResult = findViewById(R.id.cardResult)
        etSearchDistrict = findViewById(R.id.etSearchDistrict)
        btnHospitals = findViewById(R.id.btnHospitals)
        layoutHelpfulTools = findViewById(R.id.layoutHelpfulTools)
        viewFlipper = findViewById(R.id.viewFlipper)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        val cardTypes = arrayOf("BPL", "APL", "Antyodaya (AAY)")
        spinnerCard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cardTypes)

        // Added 'Government Employee'
        val jobs = arrayOf("Farmer", "Laborer", "Government Employee", "Self-Employed", "Other")
        spinnerOcc.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jobs)
    }

    private fun checkEligibility() {
        val income = etIncome.text.toString().toLongOrNull() ?: 0L
        val age = etAge.text.toString().toIntOrNull() ?: 0
        val occupation = spinnerOcc.selectedItem.toString()
        val selectedCard = spinnerCard.selectedItem.toString()
        val isDisabled = cbDisability.isChecked

        val eligible = mutableListOf<String>()
        val notEligible = mutableListOf<String>()

        // Exclusion Logic for Government Employee
        if (occupation == "Government Employee") {
            notEligible.add("• Ayushman Bharat (PM-JAY): Not eligible (Government Employees have separate health schemes).")
        } else if (selectedCard == "BPL" || selectedCard == "Antyodaya (AAY)" || income <= 120000) {
            eligible.add("• Ayushman Bharat (PM-JAY)")
        } else {
            notEligible.add("• Ayushman Bharat (PM-JAY): Criteria not met (Income or Card type).")
        }

        if (age >= 60) eligible.add("• Senior Citizen Health Insurance")
        else notEligible.add("• Senior Citizen Scheme: Requires age 60+.")

        if (occupation == "Farmer" || occupation == "Laborer") eligible.add("• Worker's Welfare Health Fund")
        else if (occupation != "Government Employee") notEligible.add("• Worker's Welfare: Only for Farmers/Laborers.")

        if (isDisabled) eligible.add("• Divyangjan Health Scheme")
        else notEligible.add("• Divyangjan Scheme: Disability status required.")

        // Update color-coded UI
        tvEligible.text = if (eligible.isNotEmpty()) "✅ ELIGIBLE:\n" + eligible.joinToString("\n\n") else ""
        tvIneligible.text = if (notEligible.isNotEmpty()) "\n❌ NOT ELIGIBLE:\n" + notEligible.joinToString("\n\n") else ""

        // REVEAL Dashboard
        cardResult.visibility = View.VISIBLE
        layoutHelpfulTools.visibility = View.VISIBLE
        
        // Auto-scroll to result
        findViewById<ScrollView>(R.id.mainScrollView).smoothScrollTo(0, cardResult.top)
    }

    private fun setupStepper() {
        findViewById<Button>(R.id.btnNext1).setOnClickListener {
            if (etAge.text.toString().isNotEmpty()) { viewFlipper.showNext(); progressBar.progress = 66 }
        }
        findViewById<Button>(R.id.btnBack2).setOnClickListener { viewFlipper.showPrevious(); progressBar.progress = 33 }
        findViewById<Button>(R.id.btnNext2).setOnClickListener {
            if (etIncome.text.toString().isNotEmpty()) { viewFlipper.showNext(); progressBar.progress = 100 }
        }
        findViewById<Button>(R.id.btnBack3).setOnClickListener { viewFlipper.showPrevious(); progressBar.progress = 66 }
    }

    private fun searchHospitals() {
        val query = etSearchDistrict.text.toString().trim()
        val match = hospitalMap.entries.find { it.key.contains(query, ignoreCase = true) }
        if (match != null) {
            AlertDialog.Builder(this).setTitle("Hospitals in ${match.key}")
                .setMessage(match.value.joinToString("\n• ", prefix = "• "))
                .setPositiveButton("OK", null).show()
        } else {
            AlertDialog.Builder(this).setTitle("Notice")
                .setMessage(getString(R.string.no_hospitals_found))
                .setPositiveButton("OK", null).show()
        }
    }

    private fun showDocumentGuide() {
        try {
            val json = assets.open("documents.json").bufferedReader().use { it.readText() }
            val array = JSONObject(json).getJSONArray("documents")
            val list = StringBuilder()
            for (i in 0 until array.length()) list.append("• ").append(array.getString(i)).append("\n")
            AlertDialog.Builder(this).setTitle(getString(R.string.docs_title))
                .setMessage(list.toString()).setPositiveButton("Got it", null).show()
        } catch (e: Exception) { }
    }

    private fun restartQuiz() {
        etAge.setText(""); etIncome.setText(""); cbDisability.isChecked = false
        cardResult.visibility = View.GONE; layoutHelpfulTools.visibility = View.GONE
        viewFlipper.displayedChild = 0; progressBar.progress = 33
        findViewById<ScrollView>(R.id.mainScrollView).smoothScrollTo(0, 0)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}