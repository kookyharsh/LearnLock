# LearnLock 

LearnLock is an innovative Android application designed to turn daily phone unlocks into passive learning opportunities. By displaying a quick, AI-generated concept summary and a bite-sized quiz question whenever you unlock your device, LearnLock makes micro-learning effortless and screen time productive.

##  Key Features

* **Passive Micro-learning:** Automatically interrupts device unlocks with a quick learning card.
* **AI-Generated Content:** Concepts and quiz questions are generated dynamically using AI (supports Google Gemini, OpenAI, and OpenRouter endpoints).
* **Add Any Custom Subjects:** Add any custom subjects you want (e.g., *English Grammar*, *Advanced Mathematics*, *Quantum Physics*, *World History*).
* **Subject-Aware Quiz Generation:** 
  * Tech/Coding subjects generate fill-in-the-blank snippets and MCQ quizzes.
  * Non-technical subjects strictly generate Multiple Choice Questions (MCQs) for clean, readable formats.
* **On-Device History:** Keep track of every single concept you have read and review your answers to solidify your learning.
* **Spotlight Coachmark Tour:** A fully native onboarding overlay that guides you through the interface.
* **Sync & Schedule Window:** Configure a set timeframe (e.g., 9:00 AM to 9:00 PM) during which learning prompts are active.

---

## App Architecture & Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Modern Material 3 components, Elegant Dark Mode theme)
* **Local Database:** Room Database (caching concepts and storing quiz history)
* **Preferences Storage:** EncryptedSharedPreferences (securing Gemini API keys)
* **Background Worker:** WorkManager & BroadcastReceiver (pre-generating concepts in the background when the queue runs low)
* **Networking:** OkHttpClient

---

## Setup and Configuration

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/kookyharsh/Learnlock.git
   cd Learnlock
   ```

2. **Configure API Key:**
   * Open the app and navigate to the **API & Setup** tab.
   * Enter your **API Key** (or custom model overrides for OpenRouter/OpenAI).
   * Click **Test API Key Connection** to verify settings.

3. **Add Learning Topics:**
   * Go to **API & Setup** ➔ **My Learning Topics**.
   * Add the specific subjects you want to learn (e.g. `English`, `World History`, `Kotlin Development`).
   * Navigate to the **Learn** tab and click **Generate new concepts** to trigger your first batch!

4. **Lock Screen Overlay Permission:**
   * LearnLock will request the **Draw Over Other Apps / Overlay Permission** on launch. This allows the quiz to display immediately when you unlock your phone.
