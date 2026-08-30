package org.akj.lingo.learn.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.akj.lingo.learn.domain.model.CurriculumDialogueLine
import org.akj.lingo.learn.domain.model.CurriculumQuizItem
import org.akj.lingo.learn.domain.model.CurriculumVocabItem
import org.akj.lingo.learn.domain.model.CurriculumUnit
import org.akj.lingo.learn.domain.model.GradeBand
import org.akj.lingo.learn.domain.model.QuizQuestionType
import org.akj.lingo.learn.domain.model.SentenceStructure
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the 10-unit Grade 1 (PRIMARY) curriculum seed data bundled with the app.
 *
 * These units are editorially authored for CEFR A1 level, targeting early primary
 * school children (Grade 1, ages 6-7). Each unit covers one themed topic with:
 *  - 8 target vocabulary items (word + IPA + example sentence + Chinese scaffold)
 *  - 3 sentence structures (the week's grammar patterns)
 *  - 6-line immersive dialogue (Lingo Fox and a child character)
 *  - 5 pre-authored quiz items
 *
 * Pedagogical design principles applied:
 *  - All English learning content stays in English (words, sentences, dialogue, quiz)
 *  - Chinese hints are ONLY shown as scaffolding when the child is stuck
 *  - Themes progress from concrete/familiar (family, colours, animals) to slightly
 *    more abstract (weather, school) across the 10-week sequence
 *  - Vocabulary is CEFR A1 Dolch sight words + high-frequency thematic words
 */
@Singleton
class CurriculumAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Returns all 10 bundled Grade 1 PRIMARY curriculum units.
     * These are pre-authored, offline-available, and marked isAvailableOffline = true.
     */
    fun loadGrade1Units(): List<CurriculumUnit> = listOf(
        buildUnit_W01_MyFamily(),
        buildUnit_W02_Colours(),
        buildUnit_W03_Animals(),
        buildUnit_W04_Food(),
        buildUnit_W05_Body(),
        buildUnit_W06_Clothes(),
        buildUnit_W07_Numbers(),
        buildUnit_W08_Weather(),
        buildUnit_W09_MySchool(),
        buildUnit_W10_MyHome()
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 1: My Family 👨‍👩‍👧
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W01_MyFamily() = CurriculumUnit(
        id = "PRIMARY_W01",
        gradeBand = GradeBand.PRIMARY,
        weekNumber = 1,
        theme = "My Family",
        themeEmoji = "👨‍👩‍👧",
        contentVersion = "2026.08.1",
        isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W01_mum", "mum", "/mʌm/", "noun", "A1", "My mum is very kind.", "妈妈"),
            vocab("PRIMARY_W01_dad", "dad", "/dæd/", "noun", "A1", "My dad can cook well.", "爸爸"),
            vocab("PRIMARY_W01_sister", "sister", "/ˈsɪstə/", "noun", "A1", "My sister likes to sing.", "姐姐/妹妹"),
            vocab("PRIMARY_W01_brother", "brother", "/ˈbrʌðə/", "noun", "A1", "My brother plays football.", "哥哥/弟弟"),
            vocab("PRIMARY_W01_grandma", "grandma", "/ˈɡrænmɑː/", "noun", "A1", "Grandma tells good stories.", "奶奶/外婆"),
            vocab("PRIMARY_W01_grandpa", "grandpa", "/ˈɡrænpɑː/", "noun", "A1", "Grandpa likes to read.", "爷爷/外公"),
            vocab("PRIMARY_W01_family", "family", "/ˈfæmɪli/", "noun", "A1", "I love my family.", "家庭"),
            vocab("PRIMARY_W01_love", "love", "/lʌv/", "verb", "A1", "I love you, Mum!", "爱")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W01_S1", "This is my _____.", "This is my mum.", "这是我的___。"),
            SentenceStructure("PRIMARY_W01_S2", "My _____ is _____.", "My dad is tall.", "我的___很___。"),
            SentenceStructure("PRIMARY_W01_S3", "I love my _____.", "I love my family.", "我爱我的___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Hello! Let's talk about your family today! 🦊", emptyList()),
            CurriculumDialogueLine(2, "Tom", "Okay! This is my mum. She is very kind.", listOf("mum")),
            CurriculumDialogueLine(3, "Lingo", "That's wonderful! Who else is in your family?", emptyList()),
            CurriculumDialogueLine(4, "Tom", "My dad, my sister, and my grandma.", listOf("dad", "sister", "grandma")),
            CurriculumDialogueLine(5, "Lingo", "What a big family! Do you love your family?", listOf("family")),
            CurriculumDialogueLine(6, "Tom", "Yes! I love my family very much!", listOf("love", "family"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W01_Q1", "Who is your mum's husband?", QuizQuestionType.IMAGE_CHOOSE_WORD,
                listOf("dad", "grandpa", "brother", "uncle"), 0, "dad"),
            quiz("PRIMARY_W01_Q2", "Fill in the blank: This is my _____. (She tells good stories.)", QuizQuestionType.SPELL_FILL_BLANK,
                correctOrder = listOf("grandma"), targetWord = "grandma"),
            quiz("PRIMARY_W01_Q3", "Listen and choose: Which word do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD,
                listOf("sister", "brother", "mum", "dad"), 2, "mum"),
            quiz("PRIMARY_W01_Q4", "Read aloud: I love my family.", QuizQuestionType.READ_ALOUD, targetWord = "love"),
            quiz("PRIMARY_W01_Q5", "Put the words in order: [love / I / family / my]", QuizQuestionType.SENTENCE_ORDER,
                correctOrder = listOf("I", "love", "my", "family"), targetWord = "family")
        ),
        roleplayScenarioId = null
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 2: Colours 🌈
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W02_Colours() = CurriculumUnit(
        id = "PRIMARY_W02", gradeBand = GradeBand.PRIMARY, weekNumber = 2,
        theme = "Colours", themeEmoji = "🌈", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W02_red", "red", "/red/", "adjective", "A1", "I have a red apple.", "红色"),
            vocab("PRIMARY_W02_blue", "blue", "/bluː/", "adjective", "A1", "The sky is blue.", "蓝色"),
            vocab("PRIMARY_W02_green", "green", "/ɡriːn/", "adjective", "A1", "Grass is green.", "绿色"),
            vocab("PRIMARY_W02_yellow", "yellow", "/ˈjeləʊ/", "adjective", "A1", "The sun is yellow.", "黄色"),
            vocab("PRIMARY_W02_orange", "orange", "/ˈɒrɪndʒ/", "adjective", "A1", "She has an orange bag.", "橙色"),
            vocab("PRIMARY_W02_purple", "purple", "/ˈpɜːpl/", "adjective", "A1", "I like purple flowers.", "紫色"),
            vocab("PRIMARY_W02_pink", "pink", "/pɪŋk/", "adjective", "A1", "Her dress is pink.", "粉色"),
            vocab("PRIMARY_W02_white", "white", "/waɪt/", "adjective", "A1", "Snow is white.", "白色")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W02_S1", "The _____ is _____.", "The apple is red.", "这个___是___色的。"),
            SentenceStructure("PRIMARY_W02_S2", "I like _____.", "I like blue.", "我喜欢___色。"),
            SentenceStructure("PRIMARY_W02_S3", "What colour is the _____?", "What colour is the sky?", "___是什么颜色的？")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "I love colours! What is your favourite colour? 🌈", emptyList()),
            CurriculumDialogueLine(2, "Lily", "I like pink! My bag is pink.", listOf("pink")),
            CurriculumDialogueLine(3, "Lingo", "Pink is lovely! What colour is the sky?", emptyList()),
            CurriculumDialogueLine(4, "Lily", "The sky is blue. And grass is green!", listOf("blue", "green")),
            CurriculumDialogueLine(5, "Lingo", "What colour is the sun?", emptyList()),
            CurriculumDialogueLine(6, "Lily", "The sun is yellow! It is bright and warm.", listOf("yellow"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W02_Q1", "What colour is the sky?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("red", "blue", "green", "yellow"), 1, "blue"),
            quiz("PRIMARY_W02_Q2", "Spell it: The colour of grass.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("green"), targetWord = "green"),
            quiz("PRIMARY_W02_Q3", "Listen and choose: Which colour do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("orange", "purple", "pink", "white"), 2, "pink"),
            quiz("PRIMARY_W02_Q4", "Read aloud: The sun is yellow.", QuizQuestionType.READ_ALOUD, targetWord = "yellow"),
            quiz("PRIMARY_W02_Q5", "Order the words: [is / apple / red / The]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("The", "apple", "is", "red"), targetWord = "red")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 3: Animals 🐾
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W03_Animals() = CurriculumUnit(
        id = "PRIMARY_W03", gradeBand = GradeBand.PRIMARY, weekNumber = 3,
        theme = "Animals", themeEmoji = "🐾", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W03_cat", "cat", "/kæt/", "noun", "A1", "My cat is fluffy.", "猫"),
            vocab("PRIMARY_W03_dog", "dog", "/dɒɡ/", "noun", "A1", "The dog can run fast.", "狗"),
            vocab("PRIMARY_W03_bird", "bird", "/bɜːd/", "noun", "A1", "The bird can fly.", "鸟"),
            vocab("PRIMARY_W03_fish", "fish", "/fɪʃ/", "noun", "A1", "I see a fish in the pond.", "鱼"),
            vocab("PRIMARY_W03_rabbit", "rabbit", "/ˈræbɪt/", "noun", "A1", "The rabbit has long ears.", "兔子"),
            vocab("PRIMARY_W03_panda", "panda", "/ˈpændə/", "noun", "A1", "Pandas eat bamboo.", "熊猫"),
            vocab("PRIMARY_W03_elephant", "elephant", "/ˈelɪfənt/", "noun", "A1", "An elephant has a long trunk.", "大象"),
            vocab("PRIMARY_W03_monkey", "monkey", "/ˈmʌŋki/", "noun", "A1", "Monkeys love bananas.", "猴子")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W03_S1", "A _____ can _____.", "A bird can fly.", "___能___。"),
            SentenceStructure("PRIMARY_W03_S2", "The _____ has _____.", "The rabbit has long ears.", "___有___。"),
            SentenceStructure("PRIMARY_W03_S3", "I like _____s.", "I like cats.", "我喜欢___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Let's visit the zoo today! What animals do you see? 🦊", emptyList()),
            CurriculumDialogueLine(2, "Tom", "I see a panda! It is black and white.", listOf("panda")),
            CurriculumDialogueLine(3, "Lingo", "Pandas are amazing! What can an elephant do?", listOf("elephant")),
            CurriculumDialogueLine(4, "Tom", "An elephant has a long trunk. It can drink water.", listOf("elephant")),
            CurriculumDialogueLine(5, "Lingo", "What is your favourite animal?", emptyList()),
            CurriculumDialogueLine(6, "Tom", "I like rabbits! They have soft fur and long ears.", listOf("rabbit"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W03_Q1", "Which animal can fly?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("cat", "dog", "bird", "fish"), 2, "bird"),
            quiz("PRIMARY_W03_Q2", "Spell it: This animal has a long trunk.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("elephant"), targetWord = "elephant"),
            quiz("PRIMARY_W03_Q3", "Listen and choose: Which animal do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("monkey", "rabbit", "panda", "fish"), 0, "monkey"),
            quiz("PRIMARY_W03_Q4", "Read aloud: Monkeys love bananas.", QuizQuestionType.READ_ALOUD, targetWord = "monkey"),
            quiz("PRIMARY_W03_Q5", "Order the words: [can / A / fly / bird]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("A", "bird", "can", "fly"), targetWord = "bird")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 4: Food 🍎
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W04_Food() = CurriculumUnit(
        id = "PRIMARY_W04", gradeBand = GradeBand.PRIMARY, weekNumber = 4,
        theme = "Food", themeEmoji = "🍎", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W04_apple", "apple", "/ˈæpl/", "noun", "A1", "I eat an apple every day.", "苹果"),
            vocab("PRIMARY_W04_banana", "banana", "/bəˈnɑːnə/", "noun", "A1", "Monkeys love bananas.", "香蕉"),
            vocab("PRIMARY_W04_rice", "rice", "/raɪs/", "noun", "A1", "We eat rice for lunch.", "米饭"),
            vocab("PRIMARY_W04_bread", "bread", "/bred/", "noun", "A1", "I like bread with butter.", "面包"),
            vocab("PRIMARY_W04_milk", "milk", "/mɪlk/", "noun", "A1", "Milk is good for you.", "牛奶"),
            vocab("PRIMARY_W04_egg", "egg", "/eɡ/", "noun", "A1", "I have an egg for breakfast.", "鸡蛋"),
            vocab("PRIMARY_W04_cake", "cake", "/keɪk/", "noun", "A1", "We have cake on birthdays.", "蛋糕"),
            vocab("PRIMARY_W04_juice", "juice", "/dʒuːs/", "noun", "A1", "I drink orange juice.", "果汁")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W04_S1", "I like _____.", "I like apples.", "我喜欢___。"),
            SentenceStructure("PRIMARY_W04_S2", "Can I have some _____, please?", "Can I have some milk, please?", "我可以要一些___吗？"),
            SentenceStructure("PRIMARY_W04_S3", "I eat _____ for _____.", "I eat rice for lunch.", "我___吃___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "I am hungry! Let's talk about food! 🍽️", emptyList()),
            CurriculumDialogueLine(2, "Lily", "I like apples! They are red and sweet.", listOf("apple")),
            CurriculumDialogueLine(3, "Lingo", "Yummy! What do you eat for breakfast?", emptyList()),
            CurriculumDialogueLine(4, "Lily", "I have an egg and some bread. And milk!", listOf("egg", "bread", "milk")),
            CurriculumDialogueLine(5, "Lingo", "Sounds delicious! What about lunch?", emptyList()),
            CurriculumDialogueLine(6, "Lily", "We eat rice for lunch. And sometimes cake! 🍰", listOf("rice", "cake"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W04_Q1", "What do monkeys love to eat?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("apple", "banana", "cake", "egg"), 1, "banana"),
            quiz("PRIMARY_W04_Q2", "Spell it: We drink this cold and white.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("milk"), targetWord = "milk"),
            quiz("PRIMARY_W04_Q3", "Listen and choose: Which food do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("bread", "juice", "rice", "cake"), 3, "cake"),
            quiz("PRIMARY_W04_Q4", "Read aloud: Can I have some juice, please?", QuizQuestionType.READ_ALOUD, targetWord = "juice"),
            quiz("PRIMARY_W04_Q5", "Order the words: [eat / I / rice / lunch / for]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("I", "eat", "rice", "for", "lunch"), targetWord = "rice")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 5: My Body 🖐️
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W05_Body() = CurriculumUnit(
        id = "PRIMARY_W05", gradeBand = GradeBand.PRIMARY, weekNumber = 5,
        theme = "My Body", themeEmoji = "🖐️", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W05_head", "head", "/hed/", "noun", "A1", "I nod my head to say yes.", "头"),
            vocab("PRIMARY_W05_eyes", "eyes", "/aɪz/", "noun", "A1", "I have two eyes.", "眼睛"),
            vocab("PRIMARY_W05_mouth", "mouth", "/maʊθ/", "noun", "A1", "I use my mouth to eat and talk.", "嘴"),
            vocab("PRIMARY_W05_nose", "nose", "/nəʊz/", "noun", "A1", "My nose can smell flowers.", "鼻子"),
            vocab("PRIMARY_W05_ears", "ears", "/ɪəz/", "noun", "A1", "Rabbits have big ears.", "耳朵"),
            vocab("PRIMARY_W05_hands", "hands", "/hændz/", "noun", "A1", "I wash my hands before eating.", "手"),
            vocab("PRIMARY_W05_feet", "feet", "/fiːt/", "noun", "A1", "I have two feet.", "脚"),
            vocab("PRIMARY_W05_tummy", "tummy", "/ˈtʌmi/", "noun", "A1", "My tummy is hungry!", "肚子")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W05_S1", "I have two _____.", "I have two eyes.", "我有两只___。"),
            SentenceStructure("PRIMARY_W05_S2", "I use my _____ to _____.", "I use my hands to clap.", "我用我的___来___。"),
            SentenceStructure("PRIMARY_W05_S3", "My _____ can _____.", "My nose can smell.", "我的___能___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Let's learn about our bodies! Touch your head! 🦊", listOf("head")),
            CurriculumDialogueLine(2, "Tom", "Head! I touched my head! Now what?", listOf("head")),
            CurriculumDialogueLine(3, "Lingo", "Now touch your nose! Can you smell with your nose?", listOf("nose")),
            CurriculumDialogueLine(4, "Tom", "Yes! My nose can smell flowers and food!", listOf("nose")),
            CurriculumDialogueLine(5, "Lingo", "Excellent! Now clap your hands!", listOf("hands")),
            CurriculumDialogueLine(6, "Tom", "Clap clap! I use my hands to clap! This is fun!", listOf("hands"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W05_Q1", "Which part of your body do you use to see?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("ears", "eyes", "mouth", "nose"), 1, "eyes"),
            quiz("PRIMARY_W05_Q2", "Spell it: You wash these before eating.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("hands"), targetWord = "hands"),
            quiz("PRIMARY_W05_Q3", "Listen and choose: Which body part do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("feet", "tummy", "head", "ears"), 2, "head"),
            quiz("PRIMARY_W05_Q4", "Read aloud: I have two eyes and one nose.", QuizQuestionType.READ_ALOUD, targetWord = "eyes"),
            quiz("PRIMARY_W05_Q5", "Order the words: [use / I / to / hands / clap / my]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("I", "use", "my", "hands", "to", "clap"), targetWord = "hands")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 6: Clothes 👕
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W06_Clothes() = CurriculumUnit(
        id = "PRIMARY_W06", gradeBand = GradeBand.PRIMARY, weekNumber = 6,
        theme = "Clothes", themeEmoji = "👕", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W06_shirt", "shirt", "/ʃɜːt/", "noun", "A1", "Dad wears a white shirt.", "衬衫"),
            vocab("PRIMARY_W06_trousers", "trousers", "/ˈtraʊzəz/", "noun", "A1", "My blue trousers are new.", "裤子"),
            vocab("PRIMARY_W06_dress", "dress", "/dres/", "noun", "A1", "She wears a pink dress.", "连衣裙"),
            vocab("PRIMARY_W06_shoes", "shoes", "/ʃuːz/", "noun", "A1", "My shoes are too small.", "鞋子"),
            vocab("PRIMARY_W06_hat", "hat", "/hæt/", "noun", "A1", "I wear a hat in the sun.", "帽子"),
            vocab("PRIMARY_W06_coat", "coat", "/kəʊt/", "noun", "A1", "Wear your coat. It is cold!", "外套"),
            vocab("PRIMARY_W06_socks", "socks", "/sɒks/", "noun", "A1", "My socks are red.", "袜子"),
            vocab("PRIMARY_W06_wear", "wear", "/weə/", "verb", "A1", "I wear a uniform to school.", "穿")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W06_S1", "I wear a _____ today.", "I wear a hat today.", "今天我穿了___。"),
            SentenceStructure("PRIMARY_W06_S2", "My _____ is/are _____.", "My socks are red.", "我的___是___。"),
            SentenceStructure("PRIMARY_W06_S3", "Put on your _____ . It is _____.", "Put on your coat. It is cold.", "穿上你的___。天气___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Good morning! What are you wearing today? 🦊", emptyList()),
            CurriculumDialogueLine(2, "Lily", "I wear a pink dress and white shoes!", listOf("dress", "shoes")),
            CurriculumDialogueLine(3, "Lingo", "How lovely! Is it cold outside?", emptyList()),
            CurriculumDialogueLine(4, "Lily", "Yes! Mum says put on my coat. It is cold!", listOf("coat")),
            CurriculumDialogueLine(5, "Lingo", "Good idea! What about a hat?", listOf("hat")),
            CurriculumDialogueLine(6, "Lily", "I have a yellow hat. Now I am ready! 😊", listOf("hat"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W06_Q1", "What do you wear on your feet?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("hat", "shirt", "shoes", "coat"), 2, "shoes"),
            quiz("PRIMARY_W06_Q2", "Spell it: You wear this when it is cold outside.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("coat"), targetWord = "coat"),
            quiz("PRIMARY_W06_Q3", "Listen and choose: Which item of clothing do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("socks", "dress", "trousers", "shirt"), 1, "dress"),
            quiz("PRIMARY_W06_Q4", "Read aloud: I wear a hat in the sun.", QuizQuestionType.READ_ALOUD, targetWord = "hat"),
            quiz("PRIMARY_W06_Q5", "Order the words: [coat / on / your / Put]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("Put", "on", "your", "coat"), targetWord = "coat")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 7: Numbers 🔢
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W07_Numbers() = CurriculumUnit(
        id = "PRIMARY_W07", gradeBand = GradeBand.PRIMARY, weekNumber = 7,
        theme = "Numbers", themeEmoji = "🔢", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W07_one", "one", "/wʌn/", "number", "A1", "I have one nose.", "一"),
            vocab("PRIMARY_W07_two", "two", "/tuː/", "number", "A1", "I have two eyes.", "二"),
            vocab("PRIMARY_W07_three", "three", "/θriː/", "number", "A1", "There are three cats.", "三"),
            vocab("PRIMARY_W07_four", "four", "/fɔː/", "number", "A1", "A dog has four legs.", "四"),
            vocab("PRIMARY_W07_five", "five", "/faɪv/", "number", "A1", "I have five fingers.", "五"),
            vocab("PRIMARY_W07_ten", "ten", "/ten/", "number", "A1", "I count to ten.", "十"),
            vocab("PRIMARY_W07_count", "count", "/kaʊnt/", "verb", "A1", "Let's count together!", "数数"),
            vocab("PRIMARY_W07_how_many", "how many", "/haʊ ˈmeni/", "phrase", "A1", "How many apples are there?", "多少")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W07_S1", "I have _____ _____.", "I have three cats.", "我有___个___。"),
            SentenceStructure("PRIMARY_W07_S2", "How many _____ are there?", "How many apples are there?", "有多少个___？"),
            SentenceStructure("PRIMARY_W07_S3", "There are _____ _____.", "There are four dogs.", "有___个___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Let's count! One, two, three! Can you count to five? 🦊", listOf("one", "two", "three", "five")),
            CurriculumDialogueLine(2, "Tom", "One, two, three, four, five! I did it!", listOf("one", "two", "three", "four", "five")),
            CurriculumDialogueLine(3, "Lingo", "Wonderful! How many fingers do you have?", listOf("how_many")),
            CurriculumDialogueLine(4, "Tom", "I have five fingers on each hand. That is ten!", listOf("five", "ten")),
            CurriculumDialogueLine(5, "Lingo", "Perfect! How many eyes do you have?", listOf("how_many")),
            CurriculumDialogueLine(6, "Tom", "I have two eyes! Let's count more things!", listOf("two", "count"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W07_Q1", "How many legs does a dog have?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("two", "three", "four", "five"), 2, "four"),
            quiz("PRIMARY_W07_Q2", "Spell it: The number after four.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("five"), targetWord = "five"),
            quiz("PRIMARY_W07_Q3", "Listen and choose: Which number do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("one", "three", "ten", "two"), 2, "ten"),
            quiz("PRIMARY_W07_Q4", "Read aloud: How many apples are there?", QuizQuestionType.READ_ALOUD, targetWord = "how_many"),
            quiz("PRIMARY_W07_Q5", "Order the words: [fingers / I / five / have]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("I", "have", "five", "fingers"), targetWord = "five")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 8: Weather ☀️
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W08_Weather() = CurriculumUnit(
        id = "PRIMARY_W08", gradeBand = GradeBand.PRIMARY, weekNumber = 8,
        theme = "Weather", themeEmoji = "☀️", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W08_sunny", "sunny", "/ˈsʌni/", "adjective", "A1", "It is sunny today. Let's play outside!", "晴天的"),
            vocab("PRIMARY_W08_rainy", "rainy", "/ˈreɪni/", "adjective", "A1", "It is rainy. Take an umbrella.", "下雨的"),
            vocab("PRIMARY_W08_cloudy", "cloudy", "/ˈklaʊdi/", "adjective", "A1", "It is cloudy and a bit cold.", "多云的"),
            vocab("PRIMARY_W08_windy", "windy", "/ˈwɪndi/", "adjective", "A1", "It is so windy! Hold your hat!", "有风的"),
            vocab("PRIMARY_W08_cold", "cold", "/kəʊld/", "adjective", "A1", "Brrr! It is very cold today.", "冷的"),
            vocab("PRIMARY_W08_hot", "hot", "/hɒt/", "adjective", "A1", "It is very hot in summer.", "热的"),
            vocab("PRIMARY_W08_umbrella", "umbrella", "/ʌmˈbrelə/", "noun", "A1", "I use an umbrella in the rain.", "雨伞"),
            vocab("PRIMARY_W08_today", "today", "/təˈdeɪ/", "adverb", "A1", "What is the weather like today?", "今天")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W08_S1", "It is _____ today.", "It is sunny today.", "今天天气___。"),
            SentenceStructure("PRIMARY_W08_S2", "What is the weather like _____?", "What is the weather like today?", "___天气怎么样？"),
            SentenceStructure("PRIMARY_W08_S3", "It is _____, so _____.", "It is rainy, so I take an umbrella.", "天气___，所以___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Good morning! What is the weather like today? 🦊", listOf("today")),
            CurriculumDialogueLine(2, "Lily", "It is sunny today! The sky is very blue.", listOf("sunny", "today")),
            CurriculumDialogueLine(3, "Lingo", "Wonderful! Is it hot or cold?", listOf("hot", "cold")),
            CurriculumDialogueLine(4, "Lily", "It is a bit warm but not too hot. Perfect for playing!", listOf("hot")),
            CurriculumDialogueLine(5, "Lingo", "What do you need when it is rainy?", listOf("rainy")),
            CurriculumDialogueLine(6, "Lily", "An umbrella! I use an umbrella in the rain. ☔", listOf("umbrella", "rainy"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W08_Q1", "What do you need when it rains?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("hat", "umbrella", "coat", "shoes"), 1, "umbrella"),
            quiz("PRIMARY_W08_Q2", "Spell it: The sky has no clouds. It is very ___.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("sunny"), targetWord = "sunny"),
            quiz("PRIMARY_W08_Q3", "Listen and choose: How is the weather?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("sunny", "rainy", "windy", "cloudy"), 2, "windy"),
            quiz("PRIMARY_W08_Q4", "Read aloud: What is the weather like today?", QuizQuestionType.READ_ALOUD, targetWord = "today"),
            quiz("PRIMARY_W08_Q5", "Order the words: [sunny / is / today / It]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("It", "is", "sunny", "today"), targetWord = "sunny")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 9: My School 🏫
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W09_MySchool() = CurriculumUnit(
        id = "PRIMARY_W09", gradeBand = GradeBand.PRIMARY, weekNumber = 9,
        theme = "My School", themeEmoji = "🏫", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W09_school", "school", "/skuːl/", "noun", "A1", "I go to school every day.", "学校"),
            vocab("PRIMARY_W09_teacher", "teacher", "/ˈtiːtʃə/", "noun", "A1", "My teacher is very kind.", "老师"),
            vocab("PRIMARY_W09_classroom", "classroom", "/ˈklɑːsruːm/", "noun", "A1", "Our classroom is clean.", "教室"),
            vocab("PRIMARY_W09_book", "book", "/bʊk/", "noun", "A1", "I read a book every night.", "书"),
            vocab("PRIMARY_W09_pencil", "pencil", "/ˈpensl/", "noun", "A1", "I write with a pencil.", "铅笔"),
            vocab("PRIMARY_W09_bag", "bag", "/bæɡ/", "noun", "A1", "My school bag is heavy.", "书包"),
            vocab("PRIMARY_W09_friend", "friend", "/frend/", "noun", "A1", "She is my best friend.", "朋友"),
            vocab("PRIMARY_W09_learn", "learn", "/lɜːn/", "verb", "A1", "I love to learn new things.", "学习")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W09_S1", "I go to _____ every day.", "I go to school every day.", "我每天去___。"),
            SentenceStructure("PRIMARY_W09_S2", "My _____ is _____.", "My classroom is big.", "我的___是___。"),
            SentenceStructure("PRIMARY_W09_S3", "I use a _____ to _____.", "I use a pencil to write.", "我用___来___。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Let's talk about school! Do you like school? 🦊", listOf("school")),
            CurriculumDialogueLine(2, "Tom", "Yes! I like school. My teacher is very nice.", listOf("teacher")),
            CurriculumDialogueLine(3, "Lingo", "What do you bring in your bag?", listOf("bag")),
            CurriculumDialogueLine(4, "Tom", "Books and pencils! And my lunch box.", listOf("book", "pencil")),
            CurriculumDialogueLine(5, "Lingo", "What do you learn at school?", listOf("learn")),
            CurriculumDialogueLine(6, "Tom", "I learn to read and write. And I make new friends!", listOf("learn", "friend"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W09_Q1", "Who teaches you at school?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("friend", "teacher", "mum", "dad"), 1, "teacher"),
            quiz("PRIMARY_W09_Q2", "Spell it: You write with this.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("pencil"), targetWord = "pencil"),
            quiz("PRIMARY_W09_Q3", "Listen and choose: Which school item do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("book", "bag", "pencil", "classroom"), 0, "book"),
            quiz("PRIMARY_W09_Q4", "Read aloud: I love to learn new things.", QuizQuestionType.READ_ALOUD, targetWord = "learn"),
            quiz("PRIMARY_W09_Q5", "Order the words: [school / go / every / I / to / day]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("I", "go", "to", "school", "every", "day"), targetWord = "school")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Week 10: My Home 🏠
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildUnit_W10_MyHome() = CurriculumUnit(
        id = "PRIMARY_W10", gradeBand = GradeBand.PRIMARY, weekNumber = 10,
        theme = "My Home", themeEmoji = "🏠", contentVersion = "2026.08.1", isAvailableOffline = true,
        vocabItems = listOf(
            vocab("PRIMARY_W10_home", "home", "/həʊm/", "noun", "A1", "Home is where my family is.", "家"),
            vocab("PRIMARY_W10_bedroom", "bedroom", "/ˈbedruːm/", "noun", "A1", "I sleep in my bedroom.", "卧室"),
            vocab("PRIMARY_W10_kitchen", "kitchen", "/ˈkɪtʃɪn/", "noun", "A1", "Mum cooks in the kitchen.", "厨房"),
            vocab("PRIMARY_W10_garden", "garden", "/ˈɡɑːdn/", "noun", "A1", "We have flowers in our garden.", "花园"),
            vocab("PRIMARY_W10_sofa", "sofa", "/ˈsəʊfə/", "noun", "A1", "We sit on the sofa to watch TV.", "沙发"),
            vocab("PRIMARY_W10_window", "window", "/ˈwɪndəʊ/", "noun", "A1", "I can see the garden from the window.", "窗户"),
            vocab("PRIMARY_W10_door", "door", "/dɔː/", "noun", "A1", "Please close the door.", "门"),
            vocab("PRIMARY_W10_live", "live", "/lɪv/", "verb", "A1", "I live in a house with my family.", "居住")
        ),
        sentenceStructures = listOf(
            SentenceStructure("PRIMARY_W10_S1", "I _____ in my _____.", "I sleep in my bedroom.", "我在___里___。"),
            SentenceStructure("PRIMARY_W10_S2", "We have _____ in our home.", "We have a garden in our home.", "我们家有___。"),
            SentenceStructure("PRIMARY_W10_S3", "I live in a _____ with my _____.", "I live in a house with my family.", "我和我的___住在___里。")
        ),
        dialogueLines = listOf(
            CurriculumDialogueLine(1, "Lingo", "Tell me about your home! Where do you live? 🦊", listOf("home", "live")),
            CurriculumDialogueLine(2, "Lily", "I live in a house. We have a garden!", listOf("live", "garden")),
            CurriculumDialogueLine(3, "Lingo", "A garden! What do you do in the garden?", listOf("garden")),
            CurriculumDialogueLine(4, "Lily", "I play there! And mum grows flowers.", listOf("garden")),
            CurriculumDialogueLine(5, "Lingo", "Where does your mum cook?", emptyList()),
            CurriculumDialogueLine(6, "Lily", "In the kitchen! It always smells delicious. 🍳", listOf("kitchen"))
        ),
        quizItems = listOf(
            quiz("PRIMARY_W10_Q1", "Where do you sleep?", QuizQuestionType.IMAGE_CHOOSE_WORD, listOf("kitchen", "garden", "bedroom", "sofa"), 2, "bedroom"),
            quiz("PRIMARY_W10_Q2", "Spell it: Mum cooks here.", QuizQuestionType.SPELL_FILL_BLANK, correctOrder = listOf("kitchen"), targetWord = "kitchen"),
            quiz("PRIMARY_W10_Q3", "Listen and choose: Which part of the home do you hear?", QuizQuestionType.LISTEN_CHOOSE_WORD, listOf("bedroom", "garden", "sofa", "window"), 1, "garden"),
            quiz("PRIMARY_W10_Q4", "Read aloud: I live in a house with my family.", QuizQuestionType.READ_ALOUD, targetWord = "live"),
            quiz("PRIMARY_W10_Q5", "Order the words: [in / my / sleep / I / bedroom]", QuizQuestionType.SENTENCE_ORDER, correctOrder = listOf("I", "sleep", "in", "my", "bedroom"), targetWord = "bedroom")
        )
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Builder helpers — keep call sites readable
    // ──────────────────────────────────────────────────────────────────────────

    private fun vocab(
        id: String, word: String, ipa: String, pos: String, cefr: String,
        example: String, zh: String
    ) = CurriculumVocabItem(id, word, ipa, pos, cefr, example, zh)

    private fun quiz(
        id: String,
        question: String,
        type: QuizQuestionType,
        options: List<String> = emptyList(),
        correctIndex: Int = -1,
        targetWord: String,
        correctOrder: List<String> = emptyList()
    ) = CurriculumQuizItem(id, question, type, options, correctIndex, correctOrder, targetWord)
}
