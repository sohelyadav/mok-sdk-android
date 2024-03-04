package com.unotag.mokone.carousel


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.unotag.mokone.MokSDK
import com.unotag.mokone.R
import com.unotag.mokone.carousel.data.CarouselContent
import com.unotag.mokone.carousel.data.CarouselResponse
import com.unotag.mokone.network.MokApiCallTask
import com.unotag.mokone.network.MokApiConstants
import com.unotag.mokone.utils.MokLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {


    private var carouselDelay: Int = 0


    private lateinit var infiniteViewPager: ViewPager2
    private lateinit var infiniteRecyclerAdapter: InfiniteRecyclerAdapter
    private var carouselItemList: MutableList<CarouselContent> = mutableListOf()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isAutoScrolling = false


    init {
        // Retrieve custom attributes
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CarouselView, defStyleAttr, 0)

        try {
            // Retrieve values by attribute names
            carouselDelay = typedArray.getInt(R.styleable.CarouselView_carouselDelay, 0)
        } finally {
            typedArray.recycle()
        }
        // Now, you can use carouselAttribute1 and carouselAttribute2 as needed
        MokLogger.log(MokLogger.LogLevel.ERROR,"Carousel Delay time: $carouselDelay")

        visibility = View.GONE
        coroutineScope.launch {
            delay(carouselDelay.toLong())
            getCarouselContent { result ->
                handleCarouselResult(result)
            }
        }
    }



    private fun handleCarouselResult(
        result: MokApiCallTask.ApiResult,
    ) {
        when (result) {
            is MokApiCallTask.ApiResult.Success -> {

                val response = result.response
                val gson = Gson()
                val carouselResponse: CarouselResponse =
                    gson.fromJson(response.toString(), CarouselResponse::class.java)
                if (carouselResponse.data?.isNotEmpty() == true) {
                    carouselResponse.data[0].caraousel_content
                        ?.let {
                            carouselItemList.addAll(it)
                        }
                }
                reload()
            }

            is MokApiCallTask.ApiResult.Error -> {
                MokLogger.log(MokLogger.LogLevel.ERROR, "Carousel api has error")
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "error: ${result.exception.localizedMessage}"
                )
                reload()
            }

            else -> {
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "Unknown error in Carousel api has occurred"
                )
                reload()
            }
        }
    }


    private fun reloadView() {
        // Inflate the layout containing the ViewPager
        val inflater = LayoutInflater.from(context)
        val carouselView: View = inflater.inflate(R.layout.carousel_view, this)

        // Find the ViewPager in the inflated layout
        infiniteViewPager = carouselView.findViewById(R.id.viewPager)

        // Check if the carouselItemList is not empty
        if (carouselItemList.isNotEmpty()) {
            visibility = View.VISIBLE
            val linearLayout = LinearLayout(context)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            linearLayout.layoutParams = layoutParams
            // Set up the CarouselView with the provided carouselItemList
            setupCarousel(carouselItemList)
            // Start auto-scrolling
            startAutoScroll()

        } else {
            // If the list is empty, hide the entire CarouselView
            visibility = View.GONE
            stopAutoScroll()
        }
    }

    // Public function to reload or refresh the view
    fun reload() {
        post {
            reloadView()
        }
    }

    private fun setupCarousel(carouselItemList: List<CarouselContent>) {
        this.carouselItemList.addAll(carouselItemList)

        infiniteRecyclerAdapter = InfiniteRecyclerAdapter(carouselItemList)
        infiniteViewPager.adapter = infiniteRecyclerAdapter

        // setting the current item of the infinite ViewPager to the actual first element
        infiniteViewPager.currentItem = 1

        // function for registering a callback to update the ViewPager
        // and provide a smooth flow for infinite scroll
        onInfinitePageChangeCallback(carouselItemList.size + 2)
    }

    private fun onInfinitePageChangeCallback(listSize: Int) {
        infiniteViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    when (infiniteViewPager.currentItem) {
                        listSize - 1 -> infiniteViewPager.setCurrentItem(1, false)
                        0 -> infiniteViewPager.setCurrentItem(listSize - 2, false)
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position != 0 && position != listSize - 1) {
                    // pageIndicatorView.setSelected(position-1)
                }
            }
        })
    }

    fun startAutoScroll() {
        // Start a coroutine for auto-scrolling if not already scrolling
        if (!isAutoScrolling) {
            coroutineScope.launch {
                while (true) {
                    // Delay for a specified duration
                    delay(3000L)

                    // Switch to the main thread to update UI
                    withContext(Dispatchers.Main) {
                        // Increment the current item to simulate auto-scrolling
                        infiniteViewPager.setCurrentItem(infiniteViewPager.currentItem + 1, true)
                    }
                }
            }
            isAutoScrolling = true
        }
    }

    fun stopAutoScroll() {
        coroutineScope.cancel()
        isAutoScrolling = false
    }
}

fun getCarouselContent(callback: ((MokApiCallTask.ApiResult) -> Unit)?) {
    val apiCallTask = MokApiCallTask()
    val userId = MokSDK.getUserId()

    apiCallTask.performApiCall(
        MokApiConstants.BASE_URL + MokApiConstants.URL_CAROUSEL_DATA + userId,
        MokApiCallTask.HttpMethod.GET,
        MokApiCallTask.MokRequestMethod.READ
    ) { result ->
        callback?.invoke(result)
    }
}
