"""
Phase 11: Hindi/Marathi LLM Fine-Tuning — Notebook Generator
Run this script locally to generate phase11_finetune.ipynb, then upload to Google Colab.

Usage:
    python phase11_generate_notebook.py
"""

import json

cells = []

def code_cell(source):
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": source if isinstance(source, list) else [source]
    }

def markdown_cell(source):
    return {
        "cell_type": "markdown",
        "metadata": {},
        "source": source if isinstance(source, list) else [source]
    }

# ─────────────────────────────────────────────
# CELL 0: Title
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "# 🏔️ TrailSense Phase 11 — Hindi/Marathi LLM Fine-Tuning\n"
    "> Fine-tune Gemma 2B on navigation & safety Q&A pairs in Hindi and Marathi using QLoRA/Unsloth on Google Colab T4 GPU.\n\n"
    "**Steps:** Environment → GPU Check → Load Gemma 2B → Load Dataset → Fine-Tune → Evaluate → Export GGUF"
))

# ─────────────────────────────────────────────
# CELL 1: Install dependencies
# ─────────────────────────────────────────────
cells.append(markdown_cell("## Step 1: Install Dependencies (run once, ~3-5 min)"))
cells.append(code_cell(
    '%%capture\n'
    '!pip install "unsloth[colab-new] @ git+https://github.com/unslothai/unsloth.git"\n'
    '!pip install --no-deps "xformers<0.0.27" "trl<0.9.0" peft accelerate bitsandbytes\n'
    '!pip install datasets sentencepiece protobuf\n'
    'print("✅ Dependencies installed.")\n'
))

# ─────────────────────────────────────────────
# CELL 2: GPU check
# ─────────────────────────────────────────────
cells.append(markdown_cell("## Step 2: Verify GPU"))
cells.append(code_cell(
    'import torch\n\n'
    'assert torch.cuda.is_available(), "❌ CUDA not available! Switch to GPU runtime in Colab: Runtime > Change runtime type > T4 GPU"\n\n'
    'gpu_name = torch.cuda.get_device_name(0)\n'
    'vram_gb = torch.cuda.get_device_properties(0).total_memory / 1e9\n'
    'print(f"✅ GPU: {gpu_name}")\n'
    'print(f"✅ VRAM: {vram_gb:.1f} GB")\n'
    'print(f"✅ CUDA version: {torch.version.cuda}")\n'
))

# ─────────────────────────────────────────────
# CELL 3: HuggingFace login
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 3: HuggingFace Login\n"
    "> Paste your HuggingFace token below. Get it from: https://huggingface.co/settings/tokens\n"
    "> Also accept Gemma's license at: https://huggingface.co/google/gemma-2b"
))
cells.append(code_cell(
    'from huggingface_hub import login\n'
    'from getpass import getpass\n\n'
    'token = getpass("Enter your HuggingFace token (hf_...): ")\n'
    'login(token=token)\n'
    'print("✅ Logged in to HuggingFace!")\n'
))

# ─────────────────────────────────────────────
# CELL 4: Load Llama 3.2 1B Instruct with Unsloth
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 4: Load Meta Llama 3.2 1B Instruct (4-bit QLoRA-ready)\n"
    "> Using Unsloth's optimized Llama 3.2 1B — fast mobile-friendly fine-tuning with 128k Indic token vocabulary."
))
cells.append(code_cell(
    'from unsloth import FastLanguageModel\n'
    'import torch\n\n'
    'max_seq_length = 1024  # Suitable for navigation Q&A\n'
    'dtype = None           # Auto-detect (bfloat16 on Ampere+, float16 on older)\n'
    'load_in_4bit = True    # 4-bit QLoRA — fits in T4 16GB VRAM comfortably\n\n'
    'print("Loading Llama 3.2 1B-Instruct (4-bit)...")\n'
    'model, tokenizer = FastLanguageModel.from_pretrained(\n'
    '    model_name="unsloth/Llama-3.2-1B-Instruct",\n'
    '    max_seq_length=max_seq_length,\n'
    '    dtype=dtype,\n'
    '    load_in_4bit=load_in_4bit,\n'
    ')\n'
    'print("✅ Llama 3.2 1B-Instruct loaded!")\n'
    'print(f"Model params: {sum(p.numel() for p in model.parameters()):,}")\n'
))

# ─────────────────────────────────────────────
# CELL 5: Apply LoRA adapters
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 5: Apply QLoRA Adapters\n"
    "> We only train ~0.5% of parameters — LoRA rank 16 on attention & MLP layers."
))
cells.append(code_cell(
    'model = FastLanguageModel.get_peft_model(\n'
    '    model,\n'
    '    r=16,\n'
    '    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",\n'
    '                     "gate_proj", "up_proj", "down_proj"],\n'
    '    lora_alpha=16,\n'
    '    lora_dropout=0,\n'
    '    bias="none",\n'
    '    use_gradient_checkpointing="unsloth",  # 30% more efficient\n'
    '    random_state=3407,\n'
    '    use_rslora=False,\n'
    '    loftq_config=None,\n'
    ')\n\n'
    'trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)\n'
    'total = sum(p.numel() for p in model.parameters())\n'
    'print(f"✅ LoRA applied!")\n'
    'print(f"Trainable params: {trainable:,} / {total:,} ({100*trainable/total:.2f}%)")\n'
))

# ─────────────────────────────────────────────
# CELL 6: Load and preview dataset
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 6: Load Hindi/Marathi Navigation Dataset\n"
    "> 100 instruction-tuning pairs covering shelter, water, exit, and emergency navigation in Hindi and Marathi."
))
cells.append(code_cell(
    'import json\n'
    'import urllib.request\n'
    'from datasets import Dataset\n\n'
    '# Load dataset — paste your phase11_dataset.json content here OR upload the file to Colab\n'
    '# Option A: Upload phase11_dataset.json to Colab (Files panel on left) and load it:\n'
    'try:\n'
    '    with open("phase11_dataset.json", "r", encoding="utf-8") as f:\n'
    '        raw_data = json.load(f)\n'
    '    print(f"✅ Dataset loaded from file: {len(raw_data)} examples")\n'
    'except FileNotFoundError:\n'
    '    print("❌ phase11_dataset.json not found!")\n'
    '    print("Please upload phase11_dataset.json to Colab using the Files panel (folder icon on left).")\n'
    '    raise\n\n'
    '# Preview first example\n'
    'print("\\n--- Sample example ---")\n'
    'print(f"Instruction: {raw_data[0][\'instruction\']}")\n'
    'print(f"Input: {raw_data[0][\'input\'][:100]}...")\n'
    'print(f"Output: {raw_data[0][\'output\'][:100]}...")\n'
))

# ─────────────────────────────────────────────
# CELL 7: Format dataset using Gemma Chat Template
# ─────────────────────────────────────────────
cells.append(markdown_cell("## Step 7: Format Dataset for Instruction Fine-Tuning"))
cells.append(code_cell(
    'from datasets import Dataset\n'
    'from unsloth.chat_templates import get_chat_template\n\n'
    'tokenizer = get_chat_template(\n'
    '    tokenizer,\n'
    '    chat_template="gemma",\n'
    ')\n\n'
    'def formatting_prompts_func(examples):\n'
    '    instructions = examples["instruction"]\n'
    '    inputs = examples["input"]\n'
    '    outputs = examples["output"]\n'
    '    texts = []\n'
    '    for inst, inp, out in zip(instructions, inputs, outputs):\n'
    '        messages = [\n'
    '            {"role": "user", "content": f"{inst}\\n\\nContext:\\n{inp}"},\n'
    '            {"role": "assistant", "content": out}\n'
    '        ]\n'
    '        text = tokenizer.apply_chat_template(messages, tokenize=False)\n'
    '        texts.append(text)\n'
    '    return {"text": texts}\n\n'
    '# Convert to HuggingFace Dataset\n'
    'hf_dataset = Dataset.from_list(raw_data)\n'
    'hf_dataset = hf_dataset.map(formatting_prompts_func, batched=True)\n\n'
    'print(f"✅ Dataset formatted: {len(hf_dataset)} examples")\n'
    'print(f"\\nSample formatted prompt (first 300 chars):")\n'
    'print(hf_dataset[0]["text"][:300])\n'
))

# ─────────────────────────────────────────────
# CELL 8: Before fine-tuning evaluation
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 8: Evaluate BEFORE Fine-Tuning\n"
    "> Run baseline test on Hindi and Marathi questions. Save these answers for comparison."
))
cells.append(code_cell(
    'from unsloth.chat_templates import get_chat_template\n\n'
    'FastLanguageModel.for_inference(model)\n'
    'test_questions = [\n'
    '    {"lang": "Hindi", "instruction": "मुझे सबसे नजदीकी पानी का स्रोत बताओ।", "input": "वर्तमान स्थिति: पर्वत पगडंडी। निकटतम आश्रय: शिखर आश्रय, 320m। निकटतम जल स्रोत: पर्वत झरना, 580m। निकटतम निकास: पूर्वी द्वार, 2.1km。"},\n'
    '    {"lang": "Hindi", "instruction": "मैं खो गया हूं, मुझे क्या करना चाहिए?", "input": "वर्तमान स्थिति: घना जंगल। निकटतम आश्रय: वन शरण, 400m। निकटतम जल स्रोत: वन नाला, 600m। निकटतम निकास: मुख्य मार्ग, 3km。"},\n'
    '    {"lang": "Hindi", "instruction": "आपातकाल में मुझे क्या करना चाहिए?", "input": "वर्तमान स्थिति: खुला पर्वत। निकटतम आश्रय: पर्वत केबिन, 200m। निकटतम जल स्रोत: झरना, 800m। निकटतम निकास: घाटी रास्ता, 4km。"},\n'
    '    {"lang": "Marathi", "instruction": "सर्वात जवळचा निवारा कुठे आहे?", "input": "सध्याची स्थिती: पर्वत मार्ग। निकटतम निवारा: शिखर निवारा, 280m। निकटतम जलस्त्रोत: पर्वत झरा, 520m। निकटतम बाहेर: पूर्व द्वार, 1.9km。"},\n'
    '    {"lang": "Marathi", "instruction": "मी हरवलो आहे, मला काय करावे?", "input": "सध्याची स्थिती: घनदाट जंगल। निकटतम निवारा: वन निवारा, 400m। निकटतम जलस्त्रोत: नाला, 500m। निकटतम बाहेर: मुख्य रस्ता, 2.8km。"}\n'
    ']\n'
    'before_answers = []\n'
    'print("=" * 60)\n'
    'print("BEFORE FINE-TUNING — Baseline Answers")\n'
    'print("=" * 60)\n\n'
    'for q in test_questions:\n'
    '    prompt = f"Below is an instruction that describes a trail navigation task. Write a response in the same language as the instruction.\\n\\n### Instruction:\\n{q[\'instruction\']}\\n\\n### Input:\\n{q[\'input\']}\\n\\n### Response:\\n"\n'
    '    inputs = tokenizer([prompt], return_tensors="pt").to("cuda")\n'
    '    outputs = model.generate(**inputs, max_new_tokens=150, use_cache=True)\n'
    '    full_text = tokenizer.batch_decode(outputs, skip_special_tokens=True)[0]\n'
    '    answer = full_text.split("### Response:\\n")[-1].strip() if "### Response:\\n" in full_text else full_text\n'
    '    before_answers.append(answer)\n'
    '    print(f"\\n[{q[\'lang\']}] Q: {q[\'instruction\']}\\nA: {answer}\\n" + "-"*40)\n\n'
    'print("\\n✅ Baseline answers saved for comparison.")\n'
))

# ─────────────────────────────────────────────
# CELL 9: Train
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 9: Fine-Tune with QLoRA (SFT)\n"
    "> Training takes ~30-60 minutes on Colab T4. Watch the loss decrease."
))
cells.append(code_cell(
    'from trl import SFTTrainer\n'
    'from transformers import TrainingArguments, DataCollatorForSeq2Seq\n'
    'from unsloth import is_bfloat16_supported\n\n'
    'FastLanguageModel.for_training(model)  # Switch back to training mode\n\n'
    'trainer = SFTTrainer(\n'
    '    model=model,\n'
    '    tokenizer=tokenizer,\n'
    '    train_dataset=hf_dataset,\n'
    '    dataset_text_field="text",\n'
    '    max_seq_length=max_seq_length,\n'
    '    dataset_num_proc=2,\n'
    '    args=TrainingArguments(\n'
    '        per_device_train_batch_size=2,\n'
    '        gradient_accumulation_steps=4,  # Effective batch size = 8\n'
    '        warmup_steps=10,\n'
    '        num_train_epochs=5,             # 5 epochs for optimal convergence\n'
    '        learning_rate=1e-4,             # Stable learning rate for Gemma 2B\n'
    '        max_grad_norm=1.0,              # Gradient clipping to prevent loss explosion\n'
    '        fp16=not is_bfloat16_supported(),\n'
    '        bf16=is_bfloat16_supported(),\n'
    '        logging_steps=5,\n'
    '        optim="adamw_8bit",\n'
    '        weight_decay=0.01,\n'
    '        lr_scheduler_type="cosine",\n'
    '        seed=3407,\n'
    '        output_dir="trailsense_lora_output",\n'
    '        report_to="none",\n'
    '    ),\n'
    ')\n\n'
    'print("🚀 Starting fine-tuning...")\n'
    'trainer_stats = trainer.train()\n\n'
    'print("\\n✅ Fine-tuning complete!")\n'
    'print(f"Training time: {trainer_stats.metrics[\'train_runtime\']:.0f}s")\n'
    'print(f"Final loss: {trainer_stats.metrics[\'train_loss\']:.4f}")\n'
))

# ─────────────────────────────────────────────
# CELL 10: After fine-tuning evaluation
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 10: Evaluate AFTER Fine-Tuning\n"
    "> Compare answers with baseline. The fine-tuned model should give more fluent and accurate Hindi/Marathi answers."
))
cells.append(code_cell(
    'FastLanguageModel.for_inference(model)\n\n'
    'after_answers = []\n'
    'print("=" * 60)\n'
    'print("AFTER FINE-TUNING — Improved Answers")\n'
    'print("=" * 60)\n\n'
    'for i, q in enumerate(test_questions):\n'
    '    prompt = f"Below is an instruction that describes a trail navigation task. Write a response in the same language as the instruction.\\n\\n### Instruction:\\n{q[\'instruction\']}\\n\\n### Input:\\n{q[\'input\']}\\n\\n### Response:\\n"\n'
    '    inputs = tokenizer([prompt], return_tensors="pt").to("cuda")\n'
    '    outputs = model.generate(**inputs, max_new_tokens=150, use_cache=True)\n'
    '    full_text = tokenizer.batch_decode(outputs, skip_special_tokens=True)[0]\n'
    '    answer = full_text.split("### Response:\\n")[-1].strip() if "### Response:\\n" in full_text else full_text\n'
    '    after_answers.append(answer)\n'
    '    print(f"\\n[{q[\'lang\']}] Q: {q[\'instruction\']}")\n'
    '    print(f"BEFORE: {before_answers[i][:150]}")\n'
    '    print(f"AFTER:  {answer[:150]}")\n'
    '    print("-" * 40)\n\n'
    'print("\\n✅ Comparison complete. Review the before/after answers above.")\n'
    'print("If AFTER answers are more fluent and accurate, proceed to export.")\n'
    'print("If not, consider more training epochs or a larger dataset.")\n'
))

# ─────────────────────────────────────────────
# CELL 11: Save LoRA adapter
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 11: Save LoRA Adapter\n"
    "> Save just the LoRA weights (small, ~100MB) before merging."
))
cells.append(code_cell(
    '# Save LoRA adapter weights only\n'
    'model.save_pretrained("trailsense_lora_adapter")\n'
    'tokenizer.save_pretrained("trailsense_lora_adapter")\n'
    'print("✅ LoRA adapter saved to: trailsense_lora_adapter/")\n'
))

# ─────────────────────────────────────────────
# CELL 12: Export GGUF
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 12: Merge & Export to GGUF (4-bit Q4_K_M)\n"
    "> Merge LoRA into base model and export to GGUF format for Android deployment.\n"
    "> This creates a ~1.5GB quantized model file."
))
cells.append(code_cell(
    '# Merge LoRA adapters into base model and export GGUF\n'
    'print("Merging LoRA and exporting to GGUF Q4_K_M...")\n'
    'print("This may take 5-10 minutes...")\n\n'
    'model.save_pretrained_gguf(\n'
    '    "trailsense_llama3.2_hindi_marathi",\n'
    '    tokenizer,\n'
    '    quantization_method="q4_k_m"  # 4-bit, good balance of size/quality\n'
    ')\n\n'
    'import os\n'
    'gguf_files = [f for f in os.listdir(".") if f.endswith(".gguf")]\n'
    'for f in gguf_files:\n'
    '    size_mb = os.path.getsize(f) / 1e6\n'
    '    print(f"✅ GGUF exported: {f} ({size_mb:.0f} MB)")\n'
))

# ─────────────────────────────────────────────
# CELL 13: Download
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## Step 13: Download GGUF Model\n"
    "> Download the fine-tuned GGUF model file to your local machine."
))
cells.append(code_cell(
    'from google.colab import files\n'
    'import glob\n\n'
    'gguf_files = glob.glob("*.gguf")\n'
    'if gguf_files:\n'
    '    gguf_path = gguf_files[0]\n'
    '    size_mb = os.path.getsize(gguf_path) / 1e6\n'
    '    print(f"Downloading: {gguf_path} ({size_mb:.0f} MB)...")\n'
    '    files.download(gguf_path)\n'
    '    print("✅ Download started!")\n'
    'else:\n'
    '    print("❌ No GGUF file found. Run Step 12 first.")\n'
))

# ─────────────────────────────────────────────
# CELL 14: Next steps
# ─────────────────────────────────────────────
cells.append(markdown_cell(
    "## ✅ Phase 11 Complete!\n\n"
    "### What to do next:\n"
    "1. **Review** the before/after answers (Step 10) — approve only if quality improved\n"
    "2. **Download** the `trailsense_hindi_marathi-Q4_K_M.gguf` file (Step 13)\n"
    "3. **Share the GGUF file path** with your developer — they will integrate it into the Android app\n\n"
    "### Android Integration (Phase 11b):\n"
    "- Replace the existing MediaPipe model with the fine-tuned GGUF\n"
    "- Switch `LlmAssistant.java` to use llama.cpp/MLC-LLM for GGUF inference\n"
    "- Re-run Phase 7 offline verification in Hindi and Marathi\n\n"
    "> **Note:** If the fine-tuned model does NOT show genuine improvement, do NOT replace the English model."
))

# ─────────────────────────────────────────────
# Build notebook JSON
# ─────────────────────────────────────────────
notebook = {
    "nbformat": 4,
    "nbformat_minor": 4,
    "metadata": {
        "accelerator": "GPU",
        "colab": {
            "gpuType": "T4",
            "name": "TrailSense_Phase11_Hindi_Marathi_Finetune.ipynb",
            "provenance": []
        },
        "kernelspec": {
            "display_name": "Python 3",
            "name": "python3"
        },
        "language_info": {
            "name": "python"
        }
    },
    "cells": cells
}

output_path = "phase11_finetune.ipynb"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(notebook, f, ensure_ascii=False, indent=2)

import sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

print(f"[OK] Notebook generated: {output_path}")
print(f"     {len(cells)} cells total")
print()
print("Next steps:")
print("  1. Go to https://colab.research.google.com")
print("  2. File > Upload notebook > select 'phase11_finetune.ipynb'")
print("  3. Runtime > Change runtime type > T4 GPU > Save")
print("  4. Upload 'phase11_dataset.json' using the Files panel (folder icon on left)")
print("  5. Run all cells in order (Runtime > Run all)")
