package it.unipi.dii.emotion_tracker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RankingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RankingFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view=inflater.inflate(R.layout.fragment_ranking, container, false)
        listView = view.findViewById<ListView>(R.id.ranking_list)

        val data = (activity as MapActivity).getData()
        println(data)
        listView.adapter = MyAdapter(requireContext(), data as List<ClusterCentroid>)
        return view
    }

    private class MyAdapter(context: Context, private val data: List<ClusterCentroid>) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)

        override fun getCount() = data.size

        override fun getItem(position: Int) = data[position]

        override fun getItemId(position: Int) = position.toLong()

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(R.layout.fragment_ranking, parent, false)

            val itemTextView = view.findViewById<TextView>(R.id.item_text_view)
            val cluster=data[position]
            itemTextView.text = "lat:${cluster.latitude}\n" +
                    "long:${cluster.longitude}\n" +
                    "street:${cluster.street}\n" +
                    "city:${cluster.city}\n" +
                    "emotion:${cluster.emotion}\n" +
                    "date:${cluster.date}\n" +
                    "numberPoints:${cluster.numberOfPoints}"

            return view
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RankingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RankingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}


