package com.example.filmer.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmer.App
import com.example.filmer.R
import com.example.filmer.views.rvadapters.RAdapter
import com.example.filmer.views.rvadapters.RItemDecorator
import com.example.filmer.views.rvadapters.RItemInteraction
import com.example.filmer.util.FilmSearch
import com.example.filmer.data.FilmData
import com.example.filmer.databinding.FragmentTvBinding
import com.example.filmer.util.AnimationHelper
import com.example.filmer.viewmodel.TVFragmentViewModel
import com.example.filmer.views.MainActivity
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TVFragment(onlyFavorites: Boolean = false) : Fragment() {
    private lateinit var binding: FragmentTvBinding
    private lateinit var adapter: RAdapter

    @Inject
    lateinit var filmSearch: Lazy<FilmSearch>
    private var lastSearch: String? = null

    private val viewModel by lazy {
        ViewModelProvider.NewInstanceFactory().create(TVFragmentViewModel::class.java)
    }

    private var filmsDataBase = listOf<FilmData>()
        set(value) {
            if (field == value) return
            field = value
            updateSearch()
        }

    var onlyFavorites: Boolean = onlyFavorites
        set(value) {
            field = value
            updateSearch()
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.instance.appComponent.inject(this)

        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.filmListData.collect {
                    withContext(Dispatchers.Main) {
                        filmsDataBase = it
                    }
                }
            }
        }
        viewModel.viewModelScope.launch {
            for(element in viewModel.showProgressBar) {
                launch(Dispatchers.Main) {
                    binding.pullToRefresh.isRefreshing = element
                }
            }
        }

        AnimationHelper.performFragmentCircularRevealAnimation(
            binding.tvFragmentRoot,
            requireActivity(),
            if (onlyFavorites) 2 else 1
        )

        val recyc = binding.RView

        recyc.layoutManager = LinearLayoutManager(requireContext())

        adapter = RAdapter(object : RAdapter.OnItemClickListener {
            override fun click(film: FilmData) {
                (requireActivity() as MainActivity).launchDetailsFragment(film)
            }
        })

        recyc.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (onlyFavorites) return
                if ((adapter.data.size - (binding.RView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()) < 20) {
                    viewModel.loadNewFilmList()
                }
            }
        })

        val dataList = ArrayList<FilmData>()
        dataList.addAll(filmsDataBase)

        adapter.data = dataList
        recyc.adapter = adapter
        recyc.addItemDecoration(RItemDecorator(5))

        val callback = RItemInteraction(binding.RView.adapter as RAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.RView)

        val anim =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.recyc_animation_anim)
        recyc.layoutAnimation = anim

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filmSearch.get().search(filmsDataBase, query, adapter, onlyFavorites)
                lastSearch = query
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (filmsDataBase.size < 1000) {
                    filmSearch.get().search(filmsDataBase, newText, adapter, onlyFavorites)
                    lastSearch = newText
                }
                return true
            }
        })
        binding.searchView.setOnClickListener {
            binding.searchView.isIconified = false
        }

        initPullToRefresh()
    }

    private fun updateSearch() {
        filmSearch.get().search(filmsDataBase, lastSearch, adapter, onlyFavorites)
        if (adapter.data.size < 20) {
            viewModel.loadNewFilmList()
        }
    }

    private fun initPullToRefresh() {
        binding.pullToRefresh.setOnRefreshListener {
            viewModel.loadNewFilmList(true)
        }
    }

    fun countOfFavorites(): Int = adapter.data.count { it.isFavorite }
}