package person.shilei.musicplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import person.shilei.musicplayer.databinding.SortBottomSheetLayoutBinding
import person.shilei.musicplayer.util.ServiceObserver
import person.shilei.musicplayer.util.SortedMode

class SortBottomSheetDialog: BottomSheetDialogFragment() {
    private lateinit var binding: SortBottomSheetLayoutBinding

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

        return binding.root
    }
}