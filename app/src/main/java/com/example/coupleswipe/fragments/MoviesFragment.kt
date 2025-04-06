package com.example.coupleswipe.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.coupleswipe.R
import com.example.coupleswipe.viewModels.MoviesFilterViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

data class FilterSelection(
    val filterName: String,       // Name of the filter (e.g., "Genre")
    val selectedValues: List<Any> // Selected values (e.g., ["Action", "Comedy"])
)
class MoviesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var filtersContainer: LinearLayout
    private val filterViewModel: MoviesFilterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_movies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        filtersContainer = view.findViewById(R.id.filters_container)
        fetchMovieFilters()
    }

    private fun fetchMovieFilters() {
        val categoryName = arguments?.getString("CATEGORY_NAME") ?: ""

        db.collection("categories")
            .document(categoryName.lowercase())
            .collection("filters")
            .get()
            .addOnSuccessListener { documents ->
                filtersContainer.removeAllViews()
                for (document in documents) {
                    when (document.getString("type")) {
                        "dropdown" -> createModernDropdownFilter(document)
                        "range" -> createRangeFilter(document)
                        "checkbox" -> createCheckboxFilter(document)
                        else -> Log.w("MoviesFragment", "Unknown filter type: ${document.getString("type")}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MoviesFragment", "Error fetching filters", e)
            }
    }

    private fun createModernDropdownFilter(document: DocumentSnapshot) {
        val filterName = document.getString("name") ?: return
        val options = document.get("options") as? List<String> ?: return
        val context = requireContext()

        val title = TextView(context).apply {
            text = filterName
            textSize = 16f
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Subtitle1)
            setPadding(0, 16, 0, 8)
        }

        val textInputLayout = TextInputLayout(
            ContextThemeWrapper(context, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu)
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        val autoCompleteTextView = AutoCompleteTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = InputType.TYPE_NULL
            setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, options))
            setText(options.firstOrNull(), false)

            setOnItemClickListener { _, _, position, _ ->
                val selectedValue = options[position]
                filterViewModel.updateFilters(filterName, listOf(selectedValue))
            }
        }

        textInputLayout.addView(autoCompleteTextView)
        filtersContainer.addView(title)
        filtersContainer.addView(textInputLayout)
    }

    private fun createRangeFilter(document: DocumentSnapshot) {
        val filterName = document.getString("name") ?: "Range"
        val minValue = (document.getLong("minValue") ?: 0).toFloat()
        val maxValue = (document.getLong("maxValue") ?: 100).toFloat()
        val context = requireContext()

        val title = TextView(context).apply {
            text = filterName
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }

        val rangeText = TextView(context).apply {
            text = "$minValue - $maxValue"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // Create range slider
        val rangeSlider = com.google.android.material.slider.RangeSlider(context).apply {
            valueFrom = minValue
            valueTo = maxValue
            setValues(minValue, maxValue) // Initial values for min and max
            stepSize = 1f // Set step size if needed
            addOnChangeListener { slider, _, _ ->
                val values = slider.values
                val currentMin = values[0]
                val currentMax = values[1]
                rangeText.text = "${currentMin.toInt()} - ${currentMax.toInt()}"
                filterViewModel.updateFilters(filterName, listOf(currentMin.toInt(), currentMax.toInt()))
            }
        }

        filtersContainer.addView(title)
        filtersContainer.addView(rangeText)
        filtersContainer.addView(rangeSlider)

        // Set initial values
        filterViewModel.updateFilters(filterName, listOf(minValue.toInt(), maxValue.toInt()))
    }

    private fun createCheckboxFilter(document: DocumentSnapshot) {
        val filterName = document.getString("name") ?: return
        val options = document.get("options") as? List<String> ?: return
        val context = requireContext()

        val title = TextView(context).apply {
            text = filterName
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }

        filtersContainer.addView(title)

        options.forEach { option ->
            val checkbox = CheckBox(context).apply {
                text = option
                setOnCheckedChangeListener { _, isChecked ->
                    val currentValues = filterViewModel.getCurrentFilters()
                        .find { it.filterName == filterName }
                        ?.selectedValues?.toMutableList() ?: mutableListOf()

                    if (isChecked) {
                        currentValues.add(option)
                    } else {
                        currentValues.remove(option)
                    }
                    filterViewModel.updateFilters(filterName, currentValues)
                }
            }
            filtersContainer.addView(checkbox)
        }
    }
}