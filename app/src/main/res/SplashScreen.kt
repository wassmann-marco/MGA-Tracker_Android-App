@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FeuerwehrRot),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .padding(horizontal = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ausbildungstracker für die",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MGA",
                    color = FeuerwehrGold,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "in Niedersachsen",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // DAS LOGO-BILD (KORRIGIERT)
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Feuerwehr Logo",
                modifier = Modifier
                    .size(150.dp) // Etwas größer wirkt auf dem Splash besser
                    .background(Color.White.copy(alpha = 0.1f), shape = CircleShape) // Ein dezenter Kreis-Effekt
                    .padding(10.dp)
            )

            Spacer(Modifier.height(40.dp))

            Text(
                "Retten · Löschen · Bergen · Schützen",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}