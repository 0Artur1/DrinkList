package com.example.drinkopedia
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkopedia.viewmodel.PostViewModel
import coil.compose.rememberImagePainter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.drinkopedia.model.DrinkDetail
import com.example.drinkopedia.viewmodel.DetailsViewModel
import com.example.drinkopedia.viewmodel.SensorViewModel
import com.example.drinkopedia.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CocktailList(
    viewModel: PostViewModel,
    onItemClick: (DrinkDetail) -> Unit,
    themeViewModel: ThemeViewModel,
    sensorViewModel: SensorViewModel
) {
    val orientation = LocalConfiguration.current.orientation
    val isDarkTheme = themeViewModel.isDarkTheme
    val isConnected by viewModel.isConnected.collectAsState()

    val cardColor = if (isDarkTheme) Color(0xFF011072) else Color(0xFFFFE0B2)
    val textColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF4E342E)
    val textColor2 = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF4E342E)
    val columns = if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) 1 else 3

    val categories = listOf("Home", "Alcoholic", "Nonalcoholic")

    val selectedCategoryIndex by viewModel.selectedCategoryIndex.collectAsState()
    val pagerState = rememberPagerState(){categories.size}

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val progress by viewModel.loadingProgress.collectAsState()
    val sensorValues by sensorViewModel.sensorValues.collectAsState()

    LaunchedEffect(selectedCategoryIndex) {
        if (pagerState.currentPage != selectedCategoryIndex) {
            pagerState.scrollToPage(selectedCategoryIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedCategoryIndex != pagerState.currentPage) {
            viewModel.setSelectedCategory(pagerState.currentPage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight()) {
                val searchText by viewModel.searchText.collectAsState()
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close drawer")
                        }
                    }
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.forEachIndexed { index, category ->
                        NavigationDrawerItem(
                            label = { Text(category) },
                            selected = selectedCategoryIndex == index,
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setSelectedCategory(index)
                                    drawerState.close()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Browse drink",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                    val selectedMode by viewModel.filterMode.collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PostViewModel.FilterMode.values().forEach { mode ->
                            val label = when (mode) {
                                PostViewModel.FilterMode.NAME -> "Name"
                                PostViewModel.FilterMode.DESCRIPTION -> "Description"
                                PostViewModel.FilterMode.INGREDIENTS -> "Ingredients"
                            }

                            OutlinedButton(
                                onClick = { viewModel.setFilterMode(mode) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (mode == selectedMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (mode == selectedMode) MaterialTheme.colorScheme.primary else Color.Gray
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = searchText,
                        onValueChange = {
                            viewModel.updateSearchText(it)
                            viewModel.setSearchQuery(it)
                        },
                        placeholder = { Text("Enter name or ingredient or instruction") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark mode", modifier = Modifier.weight(1f))
                        Switch(
                            checked = themeViewModel.isDarkTheme,
                            onCheckedChange = { isChecked ->
                                themeViewModel.toggleTheme()
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Drinkopedia") },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = cardColor
                    )
                )
            },contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->

            if (progress < 1.2f) {
                AnimatedLoadingScreen(sensorValues = sensorValues, progress = progress)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardColor)
                            .height(48.dp)
                            .padding(horizontal = 0.dp)
                    ) {
                        categories.forEachIndexed { index, category ->
                            val isSelected = index == selectedCategoryIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.setSelectedCategory(index)
                                        }
                                    }
                                    .background(
                                        if (isSelected) textColor.copy(alpha = 0.2f) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) textColor else textColor2,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (index < categories.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(Color.Gray.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    if (!isConnected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No internet connection",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) { pageIndex ->
                        LaunchedEffect(pagerState.currentPage) {
                            viewModel.setSelectedCategory(pagerState.currentPage)
                            viewModel.setCategory(categories[pagerState.currentPage])
                        }
                        when (categories[pageIndex]) {
                            "Home" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.drink_icon),
                                        contentDescription = "Logo",
                                        tint = textColor,
                                        modifier = Modifier.size(120.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Welcome in Drinkopedia!",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Swipe left to uncover delicious drink recipes!",
                                        fontSize = 16.sp,
                                        color = textColor.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            "Alcoholic" -> {
                                DrinkGrid(
                                    drinks = viewModel.filtereddrinks1,
                                    columns = columns,
                                    cardColor = cardColor,
                                    textColor = textColor,
                                    onItemClick = onItemClick,
                                    viewModel = viewModel
                                )
                            }
                            "Nonalcoholic" -> {
                                DrinkGrid(
                                    drinks = viewModel.filtereddrinks2,
                                    columns = columns,
                                    cardColor = cardColor,
                                    textColor = textColor,
                                    onItemClick = onItemClick,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun DrinkGrid(
    drinks: List<DrinkDetail>,
    columns: Int,
    cardColor: Color,
    textColor: Color,
    onItemClick: (DrinkDetail) -> Unit,
    viewModel: PostViewModel
) {
    val isInternetAvailable by viewModel.isConnected.collectAsState()
    var showNoInternetMessage by remember { mutableStateOf(false) }
    if (showNoInternetMessage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAAFF0000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No internet connection",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Red, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }
        LaunchedEffect(Unit) {
            delay(2000)
            showNoInternetMessage = false
        }
    }
    if (drinks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "No data",
                    tint = Color.Gray,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No data found",
                    color = Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(drinks) { cocktail ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            width = 2.dp,
                            color = textColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (isInternetAvailable) {
                                onItemClick(cocktail)
                            } else {
                                showNoInternetMessage = true
                            }
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberImagePainter(
                                data = cocktail.strDrinkThumb,
                                builder = {
                                    placeholder(R.drawable.placeholder)
                                    error(R.drawable.placeholder)
                                }
                            ),
                            contentDescription = cocktail.strDrink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                        Text(
                            text = cocktail.strDrink ?: "",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getPhoneNumberFromContactUri(context: Context, contactUri: Uri): String? {
    var phoneNumber: String? = null
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )

            phoneCursor?.use { pCursor ->
                if (pCursor.moveToFirst()) {
                    phoneNumber = pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                }
            }
        }
    }

    return phoneNumber
}
fun openSmsApp(context: Context, phoneNumber: String, message: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phoneNumber")
        putExtra("sms_body", message)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Brak aplikacji SMS", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RequestContactPermission(
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val permission = android.Manifest.permission.READ_CONTACTS

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> onPermissionResult(granted) }
    )

    var asked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!asked) {
            launcher.launch(permission)
            asked = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocktailDetailsScreen(
    drinkId: String,
    onBackClick: () -> Unit,
    onDrinkSelected: (DrinkDetail) -> Unit = {},
    viewModel: DetailsViewModel = viewModel(),
    listViewModel: PostViewModel
) {
    val drinkDetail = viewModel.drinkDetail
    val isLoading = viewModel.isLoading
    val allDrinks = listViewModel.filtereddrinks
    val orientation = LocalConfiguration.current.orientation
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var hasContactsPermission by remember { mutableStateOf(false) }
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri != null && hasContactsPermission) {
                val phoneNumber = getPhoneNumberFromContactUri(context, uri)
                if (phoneNumber != null) {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phoneNumber")
                        val message = drinkDetail?.let { buildIngredientList(it) } ?: return@rememberLauncherForActivityResult
                        putExtra("sms_body", message)
                    }
                    context.startActivity(smsIntent)
                } else {
                    Toast.makeText(context, "Telephone number not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No permission to access contacts", Toast.LENGTH_SHORT).show()
            }
        }
    )
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasContactsPermission = granted
            if (granted) {
                pickContactLauncher.launch(null)
            } else {
                Toast.makeText(context, "Permission to access contacts was denied", Toast.LENGTH_SHORT).show()
            }
        }
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(drinkDetail?.strDrink.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (drinkDetail != null) {
                        if (hasContactsPermission) {
                            pickContactLauncher.launch(null)
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    } else {
                        Toast.makeText(context, "Drink details are missing", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send SMS")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            drinkDetail?.let { detail ->
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            //.padding(16.dp)
                            .padding(paddingValues)
                    ) {
                        DetailContent(detail)
                    }
                } else {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        val isInternetAvailable by listViewModel.isConnected.collectAsState()
                        var showNoInternetMessage by remember { mutableStateOf(false) }
                        if (showNoInternetMessage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xAAFF0000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No internet connection",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Red, shape = RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                )
                            }
                            LaunchedEffect(Unit) {
                                delay(2000)
                                showNoInternetMessage = false
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {

                            items(allDrinks) { drink ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isInternetAvailable) {
                                                viewModel.fetchDrinkDetail(drink.idDrink)
                                                onDrinkSelected(drink)
                                            } else {
                                                showNoInternetMessage = true
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = rememberImagePainter(drink.strDrinkThumb),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(text = drink.strDrink ?: "")
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            DetailContent(detail)
                        }
                    }
                }
            } ?: run {
                Text(
                    text = "Failed to load drink details.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun DetailContent(detail: DrinkDetail) {
    detail.strDrinkThumb?.let { url ->
        Image(
            painter = rememberImagePainter(url),
            contentDescription = detail.strDrink,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    Column(modifier = Modifier.padding(16.dp)) {
        detail.strDrink?.let {
            Text(text = it, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        detail.strInstructions?.let {
            Text(text = "Instructions:", style = MaterialTheme.typography.titleMedium)
            Text(text = it)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = "Ingredients:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            (1..10).forEach { i ->
                val ingredient = detail.javaClass.getDeclaredField("strIngredient$i")
                    .apply { isAccessible = true }
                    .get(detail) as? String

                val measure = detail.javaClass.getDeclaredField("strMeasure$i")
                    .apply { isAccessible = true }
                    .get(detail) as? String

                if (!ingredient.isNullOrBlank()) {
                    val imageUrl =
                        "https://www.thecocktaildb.com/images/ingredients/${ingredient.trim()}-Small.png"

                    IngredientRow(
                        imageUrl = imageUrl,
                        name = ingredient.trim(),
                        measure = measure.orEmpty().trim()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Timer(
            modifier = Modifier.fillMaxWidth()
                .fillMaxWidth()
                .padding(horizontal = 0.dp)

        )
    }
}
fun buildIngredientList(detail: DrinkDetail): String {
    val ingredients = StringBuilder("Ingredients for ${detail.strDrink}:\n")

    for (i in 1..10) {
        val ingredient = detail.javaClass.getDeclaredField("strIngredient$i")
            .apply { isAccessible = true }
            .get(detail) as? String

        val measure = detail.javaClass.getDeclaredField("strMeasure$i")
            .apply { isAccessible = true }
            .get(detail) as? String

        if (!ingredient.isNullOrBlank()) {
            ingredients.append("- ${measure.orEmpty().trim()} ${ingredient.trim()}\n")
        }
    }

    return ingredients.toString().trim()
}
@Composable
fun IngredientRow(imageUrl: String, name: String, measure: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = rememberImagePainter(
                data = imageUrl,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error_image)
                }
            ),
            contentDescription = name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Text(
            text = measure,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(min = 40.dp, max = 80.dp)
        )

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBounce)
    )

    LaunchedEffect(true) {
        startAnimation = true
        delay(2000)
        onSplashFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.drinkopedia_ikona),
            contentDescription = "Logo",
            modifier = Modifier
                .size(150.dp)
                .scale(scale.value)
        )
    }
}

@Composable
fun AnimatedSplashScreen(onAnimationEnd: () -> Unit) {
    AndroidView(factory = { context ->
        FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)

            val imageView = ImageView(context).apply {
                setImageResource(R.drawable.drink_icon)
                layoutParams = FrameLayout.LayoutParams(300, 300).apply {
                    gravity = Gravity.CENTER
                }
                alpha = 0f
                scaleX = 0.6f
                scaleY = 0.6f
                rotation = -15f
                translationY = 100f
                cameraDistance = 8000 * resources.displayMetrics.density
            }
            val highlightView = View(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(300, 300).apply {
                    gravity = Gravity.CENTER
                }
            }

            addView(imageView)
            addView(highlightView)
            val fadeIn = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f, 1f).apply {
                duration = 400
                interpolator = AccelerateInterpolator()
            }
            val scaleX = ObjectAnimator.ofFloat(imageView, View.SCALE_X, 0.6f, 1.15f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator(2f)
            }
            val scaleY = ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 0.6f, 1.15f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator(2f)
            }
            val rotate = ObjectAnimator.ofFloat(imageView, View.ROTATION, -15f, 0f).apply {
                duration = 800
                interpolator = DecelerateInterpolator()
            }
            val moveUp = ObjectAnimator.ofFloat(imageView, View.TRANSLATION_Y, 100f, 0f).apply {
                duration = 800
                interpolator = FastOutSlowInInterpolator()
            }
            val pulseScaleX = ObjectAnimator.ofFloat(imageView, View.SCALE_X, 1f, 8f, 1f).apply {
                duration = 2000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val pulseScaleY = ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 1f, 8f, 1f).apply {
                duration = 2000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val flipY = ObjectAnimator.ofFloat(imageView, View.ROTATION_Y, 0f, 10f, 0f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val highlightAnim = ObjectAnimator.ofFloat(highlightView, View.ALPHA, 0f, 0.3f, 0f).apply {
                duration = 600
                startDelay = 900
                interpolator = DecelerateInterpolator()
            }

            val animatorSet = AnimatorSet().apply {
                playTogether(fadeIn, scaleX, scaleY, rotate, moveUp)
                start()
            }

            animatorSet.doOnEnd {
                pulseScaleX.start()
                pulseScaleY.start()
                flipY.start()
                highlightAnim.start()
                postDelayed({ onAnimationEnd() }, 2000)
            }
        }
    }, modifier = Modifier.fillMaxSize())
}
@Composable
fun MainScreen(modifier: Modifier = Modifier,sharedViewModel: DetailsViewModel = viewModel(),listsViewModel: PostViewModel = viewModel(
    factory = ViewModelProvider.AndroidViewModelFactory(LocalContext.current.applicationContext as Application)
), themeViewModel: ThemeViewModel, sensorViewModel: SensorViewModel = viewModel()) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var selectedDrink by rememberSaveable(stateSaver = DrinkSaver) {
        mutableStateOf<DrinkDetail?>(null)
    }
    val context = LocalContext.current
    val activity = context as? Activity

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CocktailList(onItemClick = { drink ->
                        selectedDrink = drink
                        sharedViewModel.fetchDrinkDetail(drink.idDrink)
                        navController.navigate("details/${drink.idDrink}")
                    }, viewModel =listsViewModel, themeViewModel = themeViewModel, sensorViewModel = sensorViewModel)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    CocktailList(onItemClick = { drink ->
                        selectedDrink = drink
                        sharedViewModel.fetchDrinkDetail(drink.idDrink)
                        navController.navigate("details/${drink.idDrink}")
                    }, viewModel = listsViewModel, themeViewModel = themeViewModel, sensorViewModel = sensorViewModel)
                }
            }
        }

        composable(
            route = "details/{drinkId}",
            arguments = listOf(navArgument("drinkId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drinkId = backStackEntry.arguments?.getString("drinkId") ?: ""
            CocktailDetailsScreen(
                drinkId = drinkId,
                onBackClick = {
                    navController.popBackStack()
                },
                onDrinkSelected = { selectedDrink = it },
                viewModel = sharedViewModel,
                listViewModel = listsViewModel
            )
        }
    }
}

@Composable
fun AnimatedLoadingScreen(
    sensorValues: FloatArray,
    progress: Float
) {
    val imageSize = 96.dp
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val offsetX = remember { Animatable(0.dp, Dp.VectorConverter) }
    val offsetY = remember { Animatable(0.dp, Dp.VectorConverter) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    LaunchedEffect(sensorValues.copyOf()) {
        val sensorX = -(sensorValues.getOrNull(0) ?: 0f) * 14f
        val sensorY = (sensorValues.getOrNull(1) ?: 0f) * 18f

        with(density) {
            val maxOffsetX = ((screenWidth - imageSize) / 2).toPx()
            val maxOffsetY = ((screenHeight - imageSize) / 2).toPx()

            val currentX = offsetX.value.toPx()
            val currentY = offsetY.value.toPx()

            val targetX = (currentX + sensorX).coerceIn(-maxOffsetX, maxOffsetX)
            val targetY = (currentY + sensorY).coerceIn(-maxOffsetY, maxOffsetY)

            launch {
                offsetX.animateTo(targetX.toDp(), animationSpec = tween(50))
            }
            launch {
                offsetY.animateTo(targetY.toDp(), animationSpec = tween(50))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX.value, y = offsetY.value)
                .size(imageSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                .shadow(8.dp, CircleShape, clip = true),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.drink_icon2),
                contentDescription = "Loading Drink",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Downloading recipes...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

val DrinkSaver = listSaver<DrinkDetail?, String>(
    save = { drink ->
        if (drink == null) listOf("", "", "","","","","","","","","","","","","","","","","","","","","","") else listOf(
            drink.idDrink ?: "",
            drink.strDrink ?: "",
            drink.strDrinkThumb ?: "",
            drink.strInstructions ?: "",
            drink.strIngredient1 ?: "", drink.strIngredient2 ?: "", drink.strIngredient3 ?: "", drink.strIngredient4 ?: "", drink.strIngredient5 ?: "", drink.strIngredient6 ?: "", drink.strIngredient7 ?: "", drink.strIngredient8 ?: "", drink.strIngredient9 ?: "", drink.strIngredient10 ?: "",
            drink.strMeasure1 ?: "", drink.strMeasure2 ?: "", drink.strMeasure3 ?: "", drink.strMeasure4 ?: "", drink.strMeasure5 ?: "", drink.strMeasure6 ?: "", drink.strMeasure7 ?: "", drink.strMeasure8 ?: "", drink.strMeasure9 ?: "", drink.strMeasure10 ?: "",
        )
    },
    restore = {
        if (it[0].isEmpty()) null else DrinkDetail(
            idDrink = it[0],
            strDrink = it[1],
            strDrinkThumb = it[2],
            strInstructions = it[3],
            strIngredient1 = it[4],
            strIngredient2 = it[5],
            strIngredient3 = it[6],
            strIngredient4 = it[7],
            strIngredient5 = it[8],
            strIngredient6 = it[9],
            strIngredient7 = it[10],
            strIngredient8 = it[11],
            strIngredient9 = it[12],
            strIngredient10 = it[13],
            strMeasure1 = it[14],
            strMeasure2 = it[15],
            strMeasure3 = it[16],
            strMeasure4 = it[17],
            strMeasure5 = it[18],
            strMeasure6 = it[19],
            strMeasure7 = it[20],
            strMeasure8 = it[21],
            strMeasure9 = it[22],
            strMeasure10 = it[23]
        )
    }
)
