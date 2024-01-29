package com.example.filmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.filmer.databinding.FragmentFilmDetailsBinding

class FilmDetailsFragment : Fragment() {
    private lateinit var binding: FragmentFilmDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilmDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val film = arguments?.get("film") as RData

        binding.apply {

            Glide.with(root)
                .load(film.posterId)
                .centerCrop()
                .into(detailsPoster)

            detailsDescription.text = film.description
            detailsToolbar.title = film.title

            favoriteFab.setImageResource(if (film.isFavorite) R.drawable.baseline_favorite_24 else R.drawable.bottom_bar_favorite_icon)
            favoriteFab.setOnClickListener {
                film.isFavorite = !film.isFavorite
                favoriteFab.setImageResource(if (film.isFavorite) R.drawable.baseline_favorite_24 else R.drawable.bottom_bar_favorite_icon)
            }

            shareFab.setOnClickListener {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this film: ${film.title} \n\n ${film.description}"
                )
                intent.type = "text/plain"
                startActivity(Intent.createChooser(intent, "Share To:"))
            }

            val fabSize = favoriteFab.customSize
            val fabMargin = (detailsToolbar.minimumHeight - fabSize) / 2.0
            favoriteFab.translationY = (-fabMargin).toFloat()
            favoriteFab.translationX = (-fabMargin).toFloat()

            shareFab.translationY = (-fabMargin).toFloat()
            shareFab.translationX = (-fabMargin * 2 - fabSize).toFloat()
        }
    }
}