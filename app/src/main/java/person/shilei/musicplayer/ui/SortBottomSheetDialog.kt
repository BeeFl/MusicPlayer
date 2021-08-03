package person.shilei.musicplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import person.shilei.musicplayer.R
import person.shilei.musicplayer.databinding.SortBottomSheetLayoutBinding
import person.shilei.musicplayer.util.ServiceObserver
import person.shilei.musicplayer.util.SortedMode

class SortBottomSheetDialog: BottomSheetDialogFragment() {
    private lateinit var binding: SortBottomSheetLayoutBinding

    private val viewModel:SortBottomSheetDialogViewModel by lazy {
        ViewModelProvider(this).get(SortBottomSheetDialogViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SortBottomSheetLayoutBinding.inflate(inflater)

        binding.sortByDateNew2old.setOnClickListener {
            ServiceObserver.sortMode.value = SortedMode.ADDED_DATE
            dismiss()
        }

        binding.sortByDateOld2new.setOnClickListener {
            ServiceObserver.sortMode.value = SortedMode.ADDED_DATE_REVERSE
            dismiss()
        }

        binding.sortBySingerName.setOnClickListener {
            ServiceObserver.sortMode.value = SortedMode.SINGER_NAME
            dismiss()
        }

        when(ServiceObserver.sortMode.value){
            SortedMode.ADDED_DATE -> {
                binding.sortByDateNew2old.setBackgroundColor(resources.getColor(R.color.teal_200))
                binding.sortByDateOld2new.setBackgroundColor(resources.getColor(R.color.white))
                binding.sortBySingerName.setBackgroundColor(resources.getColor(R.color.white))
            }
            SortedMode.ADDED_DATE_REVERSE -> {
                binding.sortByDateNew2old.setBackgroundColor(resources.getColor(R.color.white))
                binding.sortByDateOld2new.setBackgroundColor(resources.getColor(R.color.teal_200))
                binding.sortBySingerName.setBackgroundColor(resources.getColor(R.color.white))
            }
            SortedMode.SINGER_NAME -> {
                binding.sortByDateNew2old.setBackgroundColor(resources.getColor(R.color.white))
                binding.sortByDateOld2new.setBackgroundColor(resources.getColor(R.color.white))
                binding.sortBySingerName.setBackgroundColor(resources.getColor(R.color.teal_200))
            }
        }


        return binding.root
    }
}

class SortBottomSheetDialogViewModel:ViewModel(){
    val currentSortedMode = MutableLiveData(SortedMode.ADDED_DATE)
}