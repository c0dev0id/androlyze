package com.c0dev0id.androlyze

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.c0dev0id.androlyze.databinding.ItemRuleBinding
import com.c0dev0id.androlyze.rules.Rule

class RulesAdapter(
    private val onToggle: (Rule, Boolean) -> Unit,
    private val onView: (Rule) -> Unit
) : ListAdapter<Rule, RulesAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            binding.textRuleName.text = rule.name
            binding.textRuleDescription.text = rule.description
            binding.switchRule.isChecked = rule.isEnabled
            binding.switchRule.setOnCheckedChangeListener { _, isChecked ->
                onToggle(rule, isChecked)
            }
            binding.buttonView.setOnClickListener { onView(rule) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRuleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Rule>() {
            override fun areItemsTheSame(oldItem: Rule, newItem: Rule) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Rule, newItem: Rule) = oldItem == newItem
        }
    }
}
