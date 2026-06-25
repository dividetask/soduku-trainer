╭─── Claude Code v2.1.191 ─────────────────────────────────────────────────────╮
│                                                    │ Tips for getting        │
│                 Welcome back Brett!                │ started                 │
│                                                    │ Run /init to create a … │
│                       ▐▛███▜▌                      │ ─────────────────────── │
│                      ▝▜█████▛▘                     │ What's new              │
│                        ▘▘ ▝▝                       │ Added `/rewind` suppor… │
│  Opus 4.7 · Claude Max · claude@logicalbuzz.com's  │ Fixed scroll position … │
│  Organization                                      │ Fixed background agent… │
│               ~/Claude/soduku-trainer              │ /release-notes for more │
╰──────────────────────────────────────────────────────────────────────────────╯


❯ I would like to create a hint button that will help train people to learn.    
  When they push the hint button I would like the program to look for Naked     
  Singles.                                                                      
  If they cannot find a Naked Single then the soduku board will flash red       
  indicating this. We will eventually add in Hidden singles, Naked pairs,       
  Hidden pairs, Pointing pairs + box-line reduction, Naked triples + hidden     
  triples, X-Wing, and Swordfish + XY-Wing. Each time we add another solve      
  technique it will look for Naked Singles first then each solve technique in   
  order until it finds the easiest. The red flashing will only happen if it     
  cannot find a technique                                                       
                                                                                
  If it finds the Naked Single then it will display Naked Single at the top of  
  the screen, then color the target cell green. It will then go through each    
  number 1-9 coloring the cell preventing the current number from showing up in 
  the target cell. Once it hits the number that is the solution it will place   
  the correct number in the cell using an orange color, then it will continue   
  through the remaining numbers. Once it has finished this animation then it    
  will turn the solved cell to blue.                                            
                                                                                
  Does this make sense?                                                         

  Searched for 1 pattern, listed 2 directories (ctrl+o to expand)

● Yes, it makes sense — to confirm I have it right:

  Trigger: New "Hint" button. On press, run a technique chain in order (just
  Naked Single for now; later Hidden Single → Naked Pair → Hidden Pair →
  Pointing/Box-Line → Naked/Hidden Triples → X-Wing → Swordfish + XY-Wing). Use
  the first one that finds something. If nothing fires, flash the board red.

  Naked Single animation (when found):
  1. Show "Naked Single" label at top of screen.
  2. Highlight target cell green.
  3. Step through digits 1–9 with a delay; for each digit that's eliminated by a
  peer in the same row/column/box, highlight that peer cell (the one causing
  the elimination) — until you hit the surviving digit.
  4. On the surviving digit, place it in the target cell in orange.
  5. Continue stepping through the remaining digits (also showing the peer that
  eliminates each).
  6. After the sweep ends, recolor the solved cell blue (committed).

  A few quick questions before I build it — I noticed you already have
  LogicSolver.kt in the domain module and a GuessColor.kt, which suggests some
  scaffolding may already be in place:

● User answered Claude's questions:
  ⎿  · How fast should the 1–9 sweep animation run per digit? → This should be 
     defined in the config file, but have a default of 500ms
     · Should pressing Hint count against the player (penalty / mistake counter 
     / score)? → Track count only, no penalty
     · What should the user see on the cell while the 1–9 sweep runs? → Please 
     put all of the numbers in the target cell as a note, then remove each 
     number as the numbers are eliminated
     · Which peer cell should be highlighted as the 'eliminator' for each digit 
     (since multiple peers can rule out the same number)? → Highlight all peers

● Got it. Let me explore the existing code to understand the structure before
  implementing.

  Read 11 files (ctrl+o to expand)

● Now I have full context. Let me create the branch and start implementing.

● Bash(git status && git branch --show-current)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     nothing to commit, working tree clean
     claude/add-sudoku-hint-button-L1spT

❯ I initially said to place an orange number in the cell once it got to the     
  correct number, I changed my mind. Do not place the correct answer in the     
  cell until the animation completes                                            

● Got it — I'll adjust. To pin down the new sequence:

● User answered Claude's questions:
  ⎿  · After the 1–9 sweep completes, how should the answer be placed? → Blue 
     directly, no orange
     · When the sweep reaches the solution digit, what should happen during that
     step (since there are no peer eliminators)? → Pause silently, no highlight

❯ I initially said to place an orange number in the cell once it got to the     
  correct number, I changed my mind. Do not place the correct answer in the     
  cell until the animation completes                                            

● Understood. One clarification before I build:

● User answered Claude's questions:
  ⎿  · During the 1–9 sweep, the solution digit shouldn't get placed in the 
     cell. What should happen when the sweep reaches the solution digit, and how
     should the final placement look? → Sweep all, then blue (no orange)
     · Once the sweep reaches the solution digit, should it continue through the
     rest of 1–9 or stop there? → Continue past solution to 9

● Clear. Implementing now. Let me check existing tests and theme for style.

  Read 4 files (ctrl+o to expand)

● Now implementing. Let me create the HintEngine first.

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/HintEngine.kt)
  ⎿  Wrote 95 lines to ../../../user/soduku-trainer/domain/src/main/kotlin/
     com/dividetask/sudokutrainer/domain/HintEngine.kt
       2 package com.dividetask.sudokutrainer.domain
       3 /**
         rid ands the easiest applicable solving technique on a partial g
       5  * returns the data needed to teach the player how the deduction
       6  *orks.
       7  * Techniques are tried in order from easiest to hardest, matchi
       8  * progression used by [LogicSolver]. Only naked singles are imp
         lemented
          changes; the remaining techniques will plug into [find] without
      10  * to callers.
     … +95 lines (ctrl+o to expand)

❯ Please put a note in CLAUDE.md to stop execution after asking questions and   
  not use the question prompt.                                                  

● I'll address the CLAUDE.md note first, then continue the hint feature.

  Listed 1 directory (ctrl+o to expand)

● Write(/home/user/soduku-trainer/CLAUDE.md)
  ⎿  Wrote 6 lines to ../../../user/soduku-trainer/CLAUDE.md
      1 # Claude Working Notes
      2 
      3 ## Interaction style
      4 
      5 - **Do not use the AskUserQuestion tool / question prompt.** When
         clarification is needed, ask the question in plain text and STOP
         execution — wait for the user to reply before continuing.
      6 - After asking a clarifying question, end the turn. Do not procee
        d with any tool calls or implementation until the user responds.

● Noted. Continuing implementation — adding hint state to GameState.
● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameState.kt)
  ⎿  Added 3 lines, removed 1 line
       8      val activeColor: GuessColor = GuessColor.DEFAULT,
       9      val history: List<Move> = emptyList(),
      10      val phase: Phase = Phase.Playing,
      11 +    val hintCount: Int = 0,                                    
      12 +    val hintUi: HintUi? = null,                                
      13  ) {
      12 -    enum class Phase { Playing, AutoSolving, Celebrating }     
      14 +    enum class Phase { Playing, AutoSolving, Celebrating, Hinti
         +ng }                                                           
      15  
      16      init {
      17          require(cells.size == 81)

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/HintUi.kt)
  ⎿  Wrote 29 lines to ../../../user/soduku-trainer/domain/src/main/kotlin/
     com/dividetask/sudokutrainer/domain/HintUi.kt
       2 package com.dividetask.sudokutrainer.domain
       3 /**
       4  * Transient UI state driving the hint animation. Lives on [Game
       5  * is updated frame-by-frame by the ViewModel; the Board renders
          it.
       6  */
       8 sealed interface HintUi {
       9     /**
      10      * Brief red flash when no technique applies. The Board pain
         ts the
     … +24 lines (ctrl+o to expand)

● Now config and viewmodel:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 2 lines
       7      val solveSpeed: Long = 500,
       8      val fireworkSpeed: Float = 4f,
       9      val fireworkDuration: Long = 120_000,
      10 +    val hintStepMs: Long = 500,                                
      11 +    val hintFlashMs: Long = 600,                               
      12  ) {
      13      companion object {
      14          fun loadFromAssets(context: Context): GameConfig {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 2 lines
      32                  solveSpeed = map["solve_speed"]?.toLongOrNull()
           ?: 500,
      33                  fireworkSpeed = map["firework_speed"]?.toFloatO
          rNull() ?: 4f,
      34                  fireworkDuration = map["firework_duration"]?.to
          LongOrNull() ?: 120_000,
      35 +                hintStepMs = map["hint_step_ms"]?.toLongOrNull(
         +) ?: 500,                                                      
      36 +                hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
         +l() ?: 600,                                                    
      37              )
      38          }
      39      }

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 8 lines
      16  # firework_duration: Total celebration duration in milliseconds
          .
      17  #                    Firework bursts are spread evenly across t
          his window.
      18  firework_duration: 120000
      19 +                                                               
      20 +# hint_step_ms: Milliseconds between each digit step (1..9) of 
         +the hint                                                       
      21 +#               animation sweep. Lower = faster.               
      22 +hint_step_ms: 500                                              
      23 +                                                               
      24 +# hint_flash_ms: Duration of the red board flash shown when the
         + hint                                                          
      25 +#                engine can't find an applicable technique.    
      26 +hint_flash_ms: 600                                             

● Now the ViewModel — implementing the animation:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 2 lines
      10  import com.dividetask.sudokutrainer.domain.GameReducer
      11  import com.dividetask.sudokutrainer.domain.GameState
      12  import com.dividetask.sudokutrainer.domain.GuessColor
      13 +import com.dividetask.sudokutrainer.domain.HintEngine          
      14 +import com.dividetask.sudokutrainer.domain.HintUi              
      15  import kotlinx.coroutines.delay
      16  import kotlinx.coroutines.flow.MutableStateFlow
      17  import kotlinx.coroutines.flow.StateFlow

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 44 lines, removed 1 line
       84      }
       85  
       86      fun onHint() {
       87 -        // No-op in v1.                                       
       87 +        val s = _state.value                                  
       88 +        if (s.phase != GameState.Phase.Playing) return        
       89 +        if (s.hintUi != null) return                          
       90 +                                                              
       91 +        // Build a grid that only trusts cells matching the pu
          +zzle's solution.                                              
       92 +        // Wrong player entries are treated as empty so the hi
          +nt engine reasons                                             
       93 +        // from a consistent partial-solution.                
       94 +        val grid = IntArray(81) { i ->                        
       95 +            val cell = s.cells[i]                             
       96 +            val v = cell.value ?: 0                           
       97 +            if (v != 0 && v == s.puzzle.solutionAt(cell.row, c
          +ell.col)) v else 0                                            
       98 +        }                                                     
       99 +                                                              
      100 +        when (val hint = HintEngine.find(grid)) {             
      101 +            null -> {                                         
      102 +                _state.value = s.copy(hintUi = HintUi.NoHintFl
          +ash)                                                          
      103 +                viewModelScope.launch {                       
      104 +                    delay(config.hintFlashMs)                 
      105 +                    _state.value = _state.value.copy(hintUi = 
          +null)                                                         
      106 +                }                                             
      107 +            }                                                 
      108 +            is HintEngine.NakedSingle -> animateNakedSingle(hi
          +nt)                                                           
      109 +        }                                                     
      110      }
      111  
      112 +    private fun animateNakedSingle(hint: HintEngine.NakedSingl
          +e) {                                                          
      113 +        viewModelScope.launch {                               
      114 +            // Lock the board, set up the visual: green target
          +, all 1..9 notes.                                             
      115 +            _state.value = GameReducer.beginNakedSingleHint(_s
          +tate.value, hint)                                             
      116 +            delay(config.hintStepMs)                          
      117 +                                                              
      118 +            for (d in 1..9) {                                 
      119 +                val eliminators = hint.eliminators[d].orEmpty(
          +)                                                             
      120 +                _state.value = GameReducer.advanceNakedSingleS
          +tep(                                                          
      121 +                    _state.value,                             
      122 +                    activeDigit = d,                          
      123 +                    eliminatorCells = eliminators,            
      124 +                    eliminate = d != hint.solutionDigit,      
      125 +                )                                             
      126 +                delay(config.hintStepMs)                      
      127 +            }                                                 
      128 +                                                              
      129 +            _state.value = GameReducer.completeNakedSingleHint
          +(_state.value, hint)                                          
      130 +        }                                                     
      131 +    }                                                         
      132 +                                                              
      133      companion object {
      134          fun factory(repository: PuzzleRepository, config: Game
           Config, difficulty: Difficulty) =
      135              object : ViewModelProvider.Factory {

● Now add reducer functions for the hint state transitions:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 66 lines
      164      fun resetBoard(state: GameState): GameState =
      165          GameState.forNewGame(state.puzzle)
      166  
      167 +    // ---- Hint animation actions ----                       
      168 +                                                              
      169 +    /**                                                       
      170 +     * Enter the naked-single hint animation: lock the phase, 
          +paint all 1..9                                                
      171 +     * as pencil marks in the target cell, and stash the UI st
          +ate. Does not                                                 
      172 +     * touch history; the cell value is committed in [complete
          +NakedSingleHint].                                             
      173 +     */                                                       
      174 +    fun beginNakedSingleHint(state: GameState, hint: HintEngin
          +e.NakedSingle): GameState {                                   
      175 +        val target = state.cells[hint.targetCell]             
      176 +        val notes = (1..9).associateWith { GuessColor.DEFAULT 
          +}                                                             
      177 +        return state                                          
      178 +            .replaceCell(target.copy(pencilMarks = notes))    
      179 +            .copy(                                            
      180 +                phase = GameState.Phase.Hinting,              
      181 +                activeDigit = null,                           
      182 +                hintUi = HintUi.NakedSingle(                  
      183 +                    targetCell = hint.targetCell,             
      184 +                    solutionDigit = hint.solutionDigit,       
      185 +                ),                                            
      186 +            )                                                 
      187 +    }                                                         
      188 +                                                              
      189 +    /**                                                       
      190 +     * Advance the sweep to the next digit. If [eliminate] is 
          +true, the                                                     
      191 +     * digit is also removed from the target cell's pencil mar
          +ks.                                                           
      192 +     */                                                       
      193 +    fun advanceNakedSingleStep(                               
      194 +        state: GameState,                                     
      195 +        activeDigit: Int,                                     
      196 +        eliminatorCells: Set<Int>,                            
      197 +        eliminate: Boolean,                                   
      198 +    ): GameState {                                            
      199 +        val ui = state.hintUi as? HintUi.NakedSingle ?: return
          + state                                                        
      200 +        var s = state                                         
      201 +        if (eliminate) {                                      
      202 +            val target = state.cells[ui.targetCell]           
      203 +            s = s.replaceCell(target.copy(pencilMarks = target
          +.pencilMarks - activeDigit))                                  
      204 +        }                                                     
      205 +        return s.copy(                                        
      206 +            hintUi = ui.copy(                                 
      207 +                activeDigit = activeDigit,                    
      208 +                eliminatorCells = eliminatorCells,            
      209 +            ),                                                
      210 +        )                                                     
      211 +    }                                                         
      212 +                                                              
      213 +    /**                                                       
      214 +     * Finish the hint: place the solution digit in Blue (Solv
          +e color),                                                     
      215 +     * clear notes, clear hint UI, bump the hint count, return
          + to Playing.                                                  
      216 +     */                                                       
      217 +    fun completeNakedSingleHint(state: GameState, hint: HintEn
          +gine.NakedSingle): GameState {                                
      218 +        val target = state.cells[hint.targetCell]             
      219 +        val placed = target.copy(                             
      220 +            value = hint.solutionDigit,                       
      221 +            valueColor = CellColor.Guess(GuessColor.Blue),    
      222 +            pencilMarks = emptyMap(),                         
      223 +        )                                                     
      224 +        return state                                          
      225 +            .replaceCell(placed)                              
      226 +            .copy(                                            
      227 +                phase = GameState.Phase.Playing,              
      228 +                hintUi = null,                                
      229 +                hintCount = state.hintCount + 1,              
      230 +            )                                                 
      231 +    }                                                         
      232 +                                                              
      233      // ---- End-of-game actions ----
      234  
      235      fun beginAutoSolve(state: GameState): GameState =

● Now update the Board to render hint highlights and red flash:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 1 line
      24  import com.dividetask.sudokutrainer.domain.Cell
      25  import com.dividetask.sudokutrainer.domain.CellColor
      26  import com.dividetask.sudokutrainer.domain.GameState
      27 +import com.dividetask.sudokutrainer.domain.HintUi              
      28  import com.dividetask.sudokutrainer.ui.theme.toColor
      29  
      30  /**

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 7 lines, removed 1 line
      42      modifier: Modifier = Modifier,
      43  ) {
      44      val density = LocalDensity.current
      45 -    val gridBg = Color.White                                   
      45 +    val hint = state.hintUi                                    
      46 +    val gridBg = when (hint) {                                 
      47 +        HintUi.NoHintFlash -> Color(0xFFFFCDD2) // red flash   
      48 +        else -> Color.White                                    
      49 +    }                                                          
      50      val gridLine = Color(0xFFB0BEC5)
      51      val gridLineThick = Color(0xFF263238)
      52      val highlight = Color(0xFFFFF59D) // pale yellow
      53 +    val targetBg = Color(0xFFA5D6A7) // hint target: green     
      54 +    val eliminatorBg = Color(0xFFFFAB91) // hint eliminator pee
         +r: light red                                                   
      55      val pencilColor = Color(0xFF607D8B)
      56  
      57      Box(

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 19 lines
      123                          }
      124                      }
      125  
      126 +                    // Hint overlays: green target + red elimi
          +nator peers.                                                  
      127 +                    if (hint is HintUi.NakedSingle) {         
      128 +                        for (idx in hint.eliminatorCells) {   
      129 +                            val r = idx / 9; val c = idx % 9  
      130 +                            drawRect(                         
      131 +                                color = eliminatorBg,         
      132 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      133 +                                size = Size(cellW, cellH),    
      134 +                            )                                 
      135 +                        }                                     
      136 +                        val tr = hint.targetCell / 9          
      137 +                        val tc = hint.targetCell % 9          
      138 +                        drawRect(                             
      139 +                            color = targetBg,                 
      140 +                            topLeft = Offset(tc * cellW, tr * 
          +cellH),                                                       
      141 +                            size = Size(cellW, cellH),        
      142 +                        )                                     
      143 +                    }                                         
      144 +                                                              
      145                      // Grid lines: thin for every cell, thick 
           for the 3x3 boxes.
      146                      for (i in 0..9) {
      147                          val isThick = i % 3 == 0

● Now add the technique label to GameScreen and a Hint button:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 7 lines
       4  import androidx.compose.foundation.layout.Box
       5  import androidx.compose.foundation.layout.Column
       6  import androidx.compose.foundation.layout.fillMaxSize
       7 +import androidx.compose.foundation.layout.fillMaxWidth         
       8 +import androidx.compose.foundation.layout.height               
       9  import androidx.compose.foundation.layout.padding
      10 +import androidx.compose.material3.MaterialTheme                
      11  import androidx.compose.material3.Scaffold
      12 +import androidx.compose.material3.Text                         
      13  import androidx.compose.runtime.Composable
      14  import androidx.compose.runtime.LaunchedEffect
      15  import androidx.compose.runtime.getValue
      16 +import androidx.compose.ui.Alignment                           
      17  import androidx.compose.ui.Modifier
      18 +import androidx.compose.ui.text.font.FontWeight                
      19  import androidx.compose.ui.unit.dp
      20  import androidx.lifecycle.compose.collectAsStateWithLifecycle
      21  import com.dividetask.sudokutrainer.domain.GameState
      22 +import com.dividetask.sudokutrainer.domain.HintUi              
      23  
      24  @Composable
      25  fun GameScreen(

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      45                      .padding(horizontal = 12.dp, vertical = 12.
          dp),
      46                  verticalArrangement = Arrangement.spacedBy(12.d
          p),
      47              ) {
      48 +                HintLabel(state)                               
      49                  Board(
      50                      state = state,
      51                      onCellTap = viewModel::onCellTapped,

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      66                      isSolveReady = viewModel.isReadyToSolve,
      67                      onUndo = viewModel::onUndo,
      68                      onClear = viewModel::onClear,
      69 +                    onHint = viewModel::onHint,                
      70                      onGiveUpOrSolve = {
      71                          if (viewModel.isReadyToSolve) viewModel
          .onSolve() else viewModel.onGiveUp()
      72                      },

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      73                  )
      74              }
      75  
      76 +            // (overlays below)                                
      77              if (state.phase == GameState.Phase.Celebrating) {
      78                  CelebrationOverlay(
      79                      speedMultiplier = viewModel.gameConfig.fire
          workSpeed,

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 23 lines
       84          }
       85      }
       86  }
       87 +                                                              
       88 +@Composable                                                   
       89 +private fun HintLabel(state: GameState) {                     
       90 +    val text = when (val h = state.hintUi) {                  
       91 +        is HintUi.NakedSingle -> "Naked Single"               
       92 +        HintUi.NoHintFlash -> "No hint available"             
       93 +        null -> ""                                            
       94 +    }                                                         
       95 +    Box(                                                      
       96 +        modifier = Modifier                                   
       97 +            .fillMaxWidth()                                   
       98 +            .height(28.dp),                                   
       99 +        contentAlignment = Alignment.Center,                  
      100 +    ) {                                                       
      101 +        if (text.isNotEmpty()) {                              
      102 +            Text(                                             
      103 +                text = text,                                  
      104 +                style = MaterialTheme.typography.titleMedium.c
          +opy(fontWeight = FontWeight.SemiBold),                        
      105 +                color = MaterialTheme.colorScheme.onBackground
          +,                                                             
      106 +            )                                                 
      107 +        }                                                     
      108 +    }                                                         
      109 +}                                                             

● Now update Controls to add a Hint button:
● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 11 lines
      177      isSolveReady: Boolean,
      178      onUndo: () -> Unit,
      179      onClear: () -> Unit,
      180 +    onHint: () -> Unit,                                       
      181      onGiveUpOrSolve: () -> Unit,
      182      modifier: Modifier = Modifier,
      183  ) {
     ...
      196              Text("Undo")
      197          }
      198          Button(
      199 +            onClick = onHint,                                 
      200 +            enabled = !busy,                                  
      201 +            modifier = Modifier.weight(1f),                   
      202 +            colors = ButtonDefaults.buttonColors(             
      203 +                containerColor = Color(0xFF2E7D32),           
      204 +            ),                                                
      205 +        ) {                                                   
      206 +            Text("Hint")                                      
      207 +        }                                                     
      208 +        Button(                                               
      209              onClick = onClear,
      210              enabled = state.phase == GameState.Phase.Playing,
      211              modifier = Modifier.weight(1f),

● Now add tests for HintEngine:

● Write(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokutr
       ainer/domain/HintEngineTest.kt)
  ⎿  Wrote 65 lines to ../../../user/soduku-trainer/domain/src/test/kotlin/
     com/dividetask/sudokutrainer/domain/HintEngineTest.kt
      1 package com.dividetask.sudokutrainer.domain
      2 
      3 import org.junit.jupiter.api.Assertions.assertEquals
      4 import org.junit.jupiter.api.Assertions.assertNotNull
      5 import org.junit.jupiter.api.Assertions.assertNull
      6 import org.junit.jupiter.api.Assertions.assertTrue
      7 import org.junit.jupiter.api.Test
      8 
      9 class HintEngineTest {
     … +64 lines (ctrl+o to expand)

● Add a reducer test for the hint state transitions:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 40 lines
      354      }
      355  
      356      @Test
      357 +    fun `naked single hint flows from begin to complete`() {  
      358 +        val s0 = initial()                                    
      359 +        // Cell (0,2): row has 5,3,_,_,7,_,_,_,_; column has 5
          +,6,8,4,7                                                      
      360 +        // Box(0) has 5,3,6,9,8. The grid forces (0,2) to be 4
          +.                                                             
      361 +        val grid = IntArray(81) { i ->                        
      362 +            val cell = s0.cells[i]                            
      363 +            cell.value ?: 0                                   
      364 +        }                                                     
      365 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      366 +        assertEquals(2, hint.targetCell) // (0,2)             
      367 +        assertEquals(4, hint.solutionDigit)                   
      368 +                                                              
      369 +        var s = GameReducer.beginNakedSingleHint(s0, hint)    
      370 +        assertEquals(GameState.Phase.Hinting, s.phase)        
      371 +        val notes = s.cellAt(0, 2).pencilMarks                
      372 +        assertEquals((1..9).toSet(), notes.keys)              
      373 +        assertTrue(s.hintUi is HintUi.NakedSingle)            
      374 +                                                              
      375 +        // Sweep: eliminate every non-solution digit.         
      376 +        for (d in 1..9) {                                     
      377 +            s = GameReducer.advanceNakedSingleStep(           
      378 +                state = s,                                    
      379 +                activeDigit = d,                              
      380 +                eliminatorCells = hint.eliminators[d].orEmpty(
          +),                                                            
      381 +                eliminate = d != hint.solutionDigit,          
      382 +            )                                                 
      383 +        }                                                     
      384 +        // Only the solution digit remains as a pencil mark.  
      385 +        assertEquals(setOf(4), s.cellAt(0, 2).pencilMarks.keys
          +)                                                             
      386 +                                                              
      387 +        s = GameReducer.completeNakedSingleHint(s, hint)      
      388 +        assertEquals(GameState.Phase.Playing, s.phase)        
      389 +        assertNull(s.hintUi)                                  
      390 +        assertEquals(4, s.cellAt(0, 2).value)                 
      391 +        assertEquals(CellColor.Guess(GuessColor.Blue), s.cellA
          +t(0, 2).valueColor)                                           
      392 +        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())      
      393 +        assertEquals(1, s.hintCount)                          
      394 +    }                                                         
      395 +                                                              
      396 +    @Test                                                     
      397      fun `resetBoard returns to starting position`() {
      398          var s = initial()
      399          s = GameReducer.selectDigit(s, 4)

  Searched for 1 pattern (ctrl+o to expand)

● Let me try a build to validate it compiles:

● Bash(./gradlew :domain:compileKotlin --quiet 2>&1 | tail -30)
  ⎿  Downloading https://services.gradle.org/distributions/gradle-8.11.1-bi
     n.zip
     .............10%.............20%.............30%.............40%......
     … +23 lines (ctrl+o to expand)

  Read 2 files (ctrl+o to expand)

● Bash(./gradlew -PincludeApp=false :domain:test --quiet 2>&1 | tail -40)
  ⎿  30 tests completed, 1 failed

     FAILURE: Build failed with an exception.
     … +10 lines (ctrl+o to expand)

● Bash(find /home/user/soduku-trainer/domain/build/test-results -name "*.xml" |
      xargs grep -l "failure\|error" 2>/dev/null | head -5)
  ⎿  /home/user/soduku-trainer/domain/build/test-results/test/TEST-com.divi
     detask.sudokutrainer.domain.GameReducerTest.xml
     /home/user/soduku-trainer/domain/build/test-results/test/TEST-com.divi
     … +5 lines (ctrl+o to expand)

  Searched for 1 pattern (ctrl+o to expand)

● My test assumed the wrong target cell. Let me fix:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 10 lines, removed 13 lines
      356      @Test
      357      fun `naked single hint flows from begin to complete`() {
      358          val s0 = initial()
      359 -        // Cell (0,2): row has 5,3,_,_,7,_,_,_,_; column has 5
          -,6,8,4,7                                                      
      360 -        // Box(0) has 5,3,6,9,8. The grid forces (0,2) to be 4
          -.                                                             
      361 -        val grid = IntArray(81) { i ->                        
      362 -            val cell = s0.cells[i]                            
      363 -            cell.value ?: 0                                   
      364 -        }                                                     
      359 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      360          val hint = HintEngine.find(grid) as HintEngine.NakedSi
           ngle
      366 -        assertEquals(2, hint.targetCell) // (0,2)             
      367 -        assertEquals(4, hint.solutionDigit)                   
      361 +        val tRow = hint.targetCell / 9                        
      362 +        val tCol = hint.targetCell % 9                        
      363 +        // The found cell must match the puzzle's solution at 
          +that index.                                                   
      364 +        assertEquals(puzzle.solutionAt(tRow, tCol), hint.solut
          +ionDigit)                                                     
      365  
      366          var s = GameReducer.beginNakedSingleHint(s0, hint)
      367          assertEquals(GameState.Phase.Hinting, s.phase)
      371 -        val notes = s.cellAt(0, 2).pencilMarks                
      368 +        val notes = s.cellAt(tRow, tCol).pencilMarks          
      369          assertEquals((1..9).toSet(), notes.keys)
      370          assertTrue(s.hintUi is HintUi.NakedSingle)
      371  
     ...
      379              )
      380          }
      381          // Only the solution digit remains as a pencil mark.
      385 -        assertEquals(setOf(4), s.cellAt(0, 2).pencilMarks.keys
          -)                                                             
      382 +        assertEquals(setOf(hint.solutionDigit), s.cellAt(tRow,
          + tCol).pencilMarks.keys)                                      
      383  
      384          s = GameReducer.completeNakedSingleHint(s, hint)
      385          assertEquals(GameState.Phase.Playing, s.phase)
      386          assertNull(s.hintUi)
      390 -        assertEquals(4, s.cellAt(0, 2).value)                 
      391 -        assertEquals(CellColor.Guess(GuessColor.Blue), s.cellA
          -t(0, 2).valueColor)                                           
      392 -        assertTrue(s.cellAt(0, 2).pencilMarks.isEmpty())      
      387 +        assertEquals(hint.solutionDigit, s.cellAt(tRow, tCol).
          +value)                                                        
      388 +        assertEquals(CellColor.Guess(GuessColor.Blue), s.cellA
          +t(tRow, tCol).valueColor)                                     
      389 +        assertTrue(s.cellAt(tRow, tCol).pencilMarks.isEmpty())
      390          assertEquals(1, s.hintCount)
      391      }
      392  

● Bash(./gradlew -PincludeApp=false :domain:test --quiet 2>&1 | tail -15)
  ⎿  (No output)

● Bash(./gradlew -PincludeApp=false :domain:test 2>&1 | tail -10)
  ⎿  > Task :domain:classes UP-TO-DATE
     > Task :domain:jar UP-TO-DATE
     > Task :domain:compileTestKotlin UP-TO-DATE
     … +7 lines (ctrl+o to expand)

● Bash(./gradlew -PincludeApp=false :domain:test --rerun-tasks 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● Domain tests pass. Let me make sure no other callsite of ActionRow exists:

  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line, removed 1 line
      87  
      88  @Composable
      89  private fun HintLabel(state: GameState) {
      90 -    val text = when (val h = state.hintUi) {                   
      90 +    val text = when (state.hintUi) {                           
      91          is HintUi.NakedSingle -> "Naked Single"
      92          HintUi.NoHintFlash -> "No hint available"
      93          null -> ""

● I need to handle wrong-entry edge case more cleanly. Updating the ViewModel:
● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 5 lines, removed 9 lines
       88          if (s.phase != GameState.Phase.Playing) return
       89          if (s.hintUi != null) return
       90  
       91 -        // Build a grid that only trusts cells matching the pu
          -zzle's solution.                                              
       92 -        // Wrong player entries are treated as empty so the hi
          -nt engine reasons                                             
       93 -        // from a consistent partial-solution.                
       94 -        val grid = IntArray(81) { i ->                        
       95 -            val cell = s.cells[i]                             
       96 -            val v = cell.value ?: 0                           
       97 -            if (v != 0 && v == s.puzzle.solutionAt(cell.row, c
          -ell.col)) v else 0                                            
       98 -        }                                                     
       91 +        // If the board has any incorrect entries, the engine 
          +would reason                                                  
       92 +        // from inconsistent state; flash red instead.        
       93 +        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
       94 +        val hint = if (s.hasIncorrectEntry) null else HintEngi
          +ne.find(grid)                                                 
       95  
      100 -        when (val hint = HintEngine.find(grid)) {             
       96 +        when (hint) {                                         
       97              null -> {
       98                  _state.value = s.copy(hintUi = HintUi.NoHintFl
           ash)
       99                  viewModelScope.launch {

  Searched for 1 pattern (ctrl+o to expand)

● Bash(./gradlew -PincludeApp=false :domain:test --rerun-tasks 2>&1 | tail -8)
  ⎿  > Task :domain:jar
     > Task :domain:compileTestKotlin
     > Task :domain:compileTestJava NO-SOURCE
     … +5 lines (ctrl+o to expand)

● Tests pass. Let me view the final state of changed files to do a self-review:

● Bash(git status && git diff --stat)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Changes not staged for commit:
       (use "git add <file>..." to update what will be committed)
     … +29 lines (ctrl+o to expand)

● Committing and pushing to the designated branch:

● Bash(git add -A && git status)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Changes to be committed:
       (use "git restore --staged <file>..." to unstage)
     … +17 lines (ctrl+o to expand)

● Bash(git log -3 --oneline)
  ⎿  857c8ec Branch closing
     aba0ab1 Version 1
     33f27f5 Initial commit

● Bash(git commit -m "$(cat <<'EOF'
      Add Hint button with Naked Single teaching animation…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 0c55f81] Add Hint button with Nak
     ed Single teaching animation
      13 files changed, 426 insertions(+), 3 deletions(-)
     … +7 lines (ctrl+o to expand)

● Bash(git push -u origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -10)
  ⎿  remote:
     remote: Create a pull request for 'claude/add-sudoku-hint-button-L1spT
     ' on GitHub by visiting:
     … +8 lines (ctrl+o to expand)

● Pushed to claude/add-sudoku-hint-button-L1spT.

  What landed:

  - HintEngine.find() (domain) — tries techniques easiest-first; only Naked
  Single is wired up, but the chain is ready for Hidden Single → … →
  Swordfish/XY-Wing to slot in via find().
  - HintUi sealed type + Phase.Hinting + hintCount on GameState.
  - GameReducer gains beginNakedSingleHint / advanceNakedSingleStep /
  completeNakedSingleHint so the animation is driven by pure state transitions.
  - GameViewModel.onHint() runs the coroutine sweep. If the board has incorrect
  entries or no technique applies, it sets HintUi.NoHintFlash for hint_flash_ms
  and clears it.
  - Board paints the target cell green, eliminator peers light red, and the
  whole grid red during the no-hint flash.
  - GameScreen shows a "Naked Single" / "No hint available" label above the
  board.
  - ActionRow gets a green Hint button between Undo and Clear.
  - Config keys hint_step_ms (500ms default) and hint_flash_ms (600ms default)
  in config.yaml/GameConfig.
  - Tests: HintEngineTest (naked-single detection + null cases) and a
  GameReducerTest covering the begin→sweep→complete flow. All 30 domain tests
  pass.

  Per your latest spec changes:
  - No digit is placed in the target during the sweep.
  - The sweep continues all the way through 9 even after passing the solution
  digit.
  - At the end the cell goes straight to Blue (no orange step).

  Also added CLAUDE.md per your request: when I need to clarify something, I'll
  ask in plain text and stop — no question prompts.

  Note: I could only validate the domain (pure-JVM) module here; the :app
  Android build needs Google Maven, which this sandbox doesn't have. Worth
  running ./gradlew :app:assembleDebug on your end before testing on a device.

● My apologies — I just violated the rule I added to CLAUDE.md by using the
  question prompt. I'll ask in plain text going forward.

  The good news: those two answers happen to match exactly what I already built.

  - Solution-digit step: in GameViewModel.animateNakedSingle,
  hint.eliminators[solutionDigit] is never populated, so eliminatorCells is
  empty and eliminate = false — that step is a silent pause with no peer
  highlights and no note removal. ✓
  - End placement: GameReducer.completeNakedSingleHint places the value directly
  with CellColor.Guess(GuessColor.Blue) — no orange intermediate. ✓

  So the pushed branch already does what you confirmed. Nothing more to change.

❯ What strategies are needed for level 2 and how would  you recommend we handle 
  teaching how to solve that way                                                

  Read 5 files, listed 1 directory (ctrl+o to expand)

● Level 2 in our grader is Hidden Singles — a digit d has only one legal cell
  within some unit (row, column, or box), because every other empty cell in that
  unit either already conflicts with d elsewhere or is filled.

  The deduction is structurally the inverse of a Naked Single, which suggests a
  mirrored teaching animation:

  Naked Single (current): fix the cell, sweep digits 1..9, show peers that kill
  each digit. Conclusion: "only one digit survives → place it."

  Hidden Single (proposed): fix the digit, then sweep the unit's empty cells,
  showing for each cell the peer in a different unit that already holds d (so d
  can't go there). One cell survives the sweep → place d there.

  Concretely, I'd extend the model like this:

  1. HintEngine.HiddenSingle with fields:
    - targetCell, solutionDigit (same shape as NakedSingle so the View layer
  treats them similarly)
    - unit: Unit — which row/col/box made the deduction (we should pick the
  smallest explanatory unit if multiple apply; box usually reads best)
    - eliminators: Map<Int, Set<Int>> — for each other empty cell in the unit,
  the peer cell(s) outside the unit that prove d can't land there. Filled cells
  in the unit need no eliminator (they're self-evidently taken).
  2. HintUi.HiddenSingle animation:
    - Tint the whole unit (row/column/box) faintly to frame the deduction's
  scope — this is the new visual concept Naked Single doesn't need.
    - Shade targetCell green from the start (consistent with NakedSingle).
    - Sweep the other empty cells in the unit one at a time: shade the current
  cell amber, highlight its blocking peer(s) outside the unit in the same red
  the eliminators use today, briefly cross out the cell.
    - Filled cells in the unit can be shown pre-crossed or just dimmed — they
  need no per-step explanation.
    - End: only targetCell remains, place solutionDigit in blue.
  3. Picking the unit when multiple work. A hidden single in a box is usually
  the easiest to see; if a row and a box both reveal the same single, prefer the
  box. I'd tie-break box > row > column.
  4. Reusing the rest of the pipeline. GameViewModel.animateNakedSingle already
  has the per-step "highlight eliminators, optionally erase notes" loop. If we
  generalize it to take a list of steps (cellToShade, eliminatorCells, 
  eliminateDigit?) then both NakedSingle and HiddenSingle animations are the
  same renderer with different step lists. Worth doing now; the higher-level
  techniques (pairs/triples) will keep fitting that mold.
  5. Pencil-mark erasure. Naked Single removes the dead digit from the target
  cell's notes. Hidden Single's natural erase is removing d from every other
  empty cell in the unit during the sweep. That's a nice visual payoff and keeps
  the "hint also tidies your notes" promise consistent.

  Tradeoff to flag: introducing the "unit frame" tint is a real new piece of
  board chrome. We could skip it and rely purely on which cells get crossed off,
  but the unit frame is what makes a hidden-single deduction feel different
  from a naked-single one — without it, players may not internalize that the
  technique scans a unit rather than a cell.

  Want me to sketch the HintEngine + HintUi data shapes as a concrete PR, or
  talk through step 4 (generalizing the animator) first?

❯ Please handle it as you see fit and I will look to see whether it should  be  
  changed                                                                       

● Bash(git status && echo "---" && git branch --show-current && echo "---" &&
      git log --oneline -10)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Your branch is up to date with 'origin/claude/add-sudoku-hint-button-L
     1spT'.
     … +9 lines (ctrl+o to expand)

  Read 7 files (ctrl+o to expand)

● Plan: add Hidden Single as the second technique. Mirror Naked Single's pattern
  — engine returns the deduction, reducer drives a step animation, board
  renders a new unit-framed UI state. Tie-break box > row > column when multiple
  units produce the same single.

  Let me check whether the build environment works before writing too much.

  Listed 1 directory (ctrl+o to expand)

● Bash(./gradlew :domain:test --no-daemon 2>&1 | tail -30)  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +23 lines (ctrl+o to expand)

  Read 1 file (ctrl+o to expand)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -20)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +15 lines (ctrl+o to expand)

● Good — domain tests build. Now I'll add Hidden Single across the engine,
  reducer, view model, and board.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 22 lines
      31          override val techniqueName: String = "Naked Single"
      32      }
      33  
      34 +    enum class UnitKind { Box, Row, Column }                   
      35 +                                                               
      36 +    /** A row, column, or 3x3 box; the unit a deduction lives i
         +n. */                                                          
      37 +    data class HintUnit(val kind: UnitKind, val cells: List<Int
         +>)                                                             
      38 +                                                               
      39      /**
      40 +     * Inside [unit], [solutionDigit] has exactly one legal cel
         +l — the                                                        
      41 +     * [targetCell]. [eliminators] maps each *other empty* cell
         + of the unit                                                   
      42 +     * to the set of peer cells outside [unit] whose values pro
         +ve the digit                                                   
      43 +     * can't go in that cell. Filled cells in the unit are self
         +-evident and                                                   
      44 +     * have no entry.                                          
      45 +     */                                                        
      46 +    data class HiddenSingle(                                   
      47 +        override val targetCell: Int,                          
      48 +        override val solutionDigit: Int,                       
      49 +        val unit: HintUnit,                                    
      50 +        val eliminators: Map<Int, Set<Int>>,                   
      51 +    ) : Hint {                                                 
      52 +        override val techniqueName: String = "Hidden Single"   
      53 +    }                                                          
      54 +                                                               
      55 +    /**                                                        
      56       * Search [grid] for the easiest technique that produces a 
          placement.
      57       * Returns null if no implemented technique applies.
      58       *
     ...
      61      fun find(grid: IntArray): Hint? {
      62          require(grid.size == 81)
      63          findNakedSingle(grid)?.let { return it }
      64 +        findHiddenSingle(grid)?.let { return it }              
      65          return null
      66      }
      67  

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 90 lines
       90          return null
       91      }
       92  
       93 +    private fun findHiddenSingle(grid: IntArray): HiddenSingle
          +? {                                                           
       94 +        // Iteration order encodes the tie-break: boxes are tr
          +ied before                                                    
       95 +        // rows, rows before columns — so when the same digit 
          +is a hidden                                                   
       96 +        // single in multiple units the box deduction wins.   
       97 +        for (unit in UNITS_ORDERED) {                         
       98 +            val unitValues = unit.cells.mapTo(mutableSetOf()) 
          +{ grid[it] }                                                  
       99 +            for (d in 1..9) {                                 
      100 +                if (d in unitValues) continue                 
      101 +                var target = -1                               
      102 +                var multiple = false                          
      103 +                for (idx in unit.cells) {                     
      104 +                    if (grid[idx] != 0) continue              
      105 +                    val row = idx / 9                         
      106 +                    val col = idx % 9                         
      107 +                    if (!SudokuSolver.isValidPlacement(grid, r
          +ow, col, d)) continue                                         
      108 +                    if (target == -1) {                       
      109 +                        target = idx                          
      110 +                    } else {                                  
      111 +                        multiple = true                       
      112 +                        break                                 
      113 +                    }                                         
      114 +                }                                             
      115 +                if (target == -1 || multiple) continue        
      116 +                val eliminators = buildMap {                  
      117 +                    val unitSet = unit.cells.toSet()          
      118 +                    for (idx in unit.cells) {                 
      119 +                        if (idx == target || grid[idx] != 0) c
          +ontinue                                                       
      120 +                        put(idx, peersOutsideUnitHolding(grid,
          + idx, unitSet, d))                                            
      121 +                    }                                         
      122 +                }                                             
      123 +                return HiddenSingle(                          
      124 +                    targetCell = target,                      
      125 +                    solutionDigit = d,                        
      126 +                    unit = unit,                              
      127 +                    eliminators = eliminators,                
      128 +                )                                             
      129 +            }                                                 
      130 +        }                                                     
      131 +        return null                                           
      132 +    }                                                         
      133 +                                                              
      134 +    /** Peer cells of [cellIdx] that lie outside [unitCells] a
          +nd hold [digit]. */                                           
      135 +    private fun peersOutsideUnitHolding(                      
      136 +        grid: IntArray,                                       
      137 +        cellIdx: Int,                                         
      138 +        unitCells: Set<Int>,                                  
      139 +        digit: Int,                                           
      140 +    ): Set<Int> {                                             
      141 +        val row = cellIdx / 9                                 
      142 +        val col = cellIdx % 9                                 
      143 +        val out = mutableSetOf<Int>()                         
      144 +        for (c in 0 until 9) {                                
      145 +            if (c == col) continue                            
      146 +            val i = row * 9 + c                               
      147 +            if (i !in unitCells && grid[i] == digit) out += i 
      148 +        }                                                     
      149 +        for (r in 0 until 9) {                                
      150 +            if (r == row) continue                            
      151 +            val i = r * 9 + col                               
      152 +            if (i !in unitCells && grid[i] == digit) out += i 
      153 +        }                                                     
      154 +        val br = (row / 3) * 3                                
      155 +        val bc = (col / 3) * 3                                
      156 +        for (r in br until br + 3) {                          
      157 +            for (c in bc until bc + 3) {                      
      158 +                if (r == row && c == col) continue            
      159 +                val i = r * 9 + c                             
      160 +                if (i !in unitCells && grid[i] == digit) out +
          += i                                                           
      161 +            }                                                 
      162 +        }                                                     
      163 +        return out                                            
      164 +    }                                                         
      165 +                                                              
      166 +    private val UNITS_ORDERED: List<HintUnit> = buildList {   
      167 +        for (br in 0 until 3) for (bc in 0 until 3) {         
      168 +            val cells = buildList {                           
      169 +                for (dr in 0 until 3) for (dc in 0 until 3) { 
      170 +                    add((br * 3 + dr) * 9 + (bc * 3 + dc))    
      171 +                }                                             
      172 +            }                                                 
      173 +            add(HintUnit(UnitKind.Box, cells))                
      174 +        }                                                     
      175 +        for (r in 0 until 9) {                                
      176 +            add(HintUnit(UnitKind.Row, (0 until 9).map { r * 9
          + + it }))                                                     
      177 +        }                                                     
      178 +        for (c in 0 until 9) {                                
      179 +            add(HintUnit(UnitKind.Column, (0 until 9).map { it
          + * 9 + c }))                                                  
      180 +        }                                                     
      181 +    }                                                         
      182 +                                                              
      183      /** All peer cells of (row,col) whose value equals [digit]
           . */
      184      private fun peersHolding(grid: IntArray, row: Int, col: In
           t, digit: Int): Set<Int> {
      185          val out = mutableSetOf<Int>()

● Now HintUi:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Added 19 lines
      26          val activeDigit: Int? = null,
      27          val eliminatorCells: Set<Int> = emptySet(),
      28      ) : HintUi
      29 +                                                               
      30 +    /**                                                        
      31 +     * Hidden-single teaching animation in progress.           
      32 +     *                                                         
      33 +     * - [unitCells] is the deduction's row/column/box; the Boa
         +rd tints it                                                    
      34 +     *   to frame the scope of the inference.                  
      35 +     * - [targetCell] is shaded green for the duration.        
      36 +     * - During the per-cell sweep, [currentCell] is the unit c
         +ell whose                                                      
      37 +     *   elimination is currently being shown, and [eliminatorC
         +ells] are                                                      
      38 +     *   the peer cells *outside* the unit that prove the solut
         +ion digit                                                      
      39 +     *   can't go there.                                       
      40 +     */                                                        
      41 +    data class HiddenSingle(                                   
      42 +        val targetCell: Int,                                   
      43 +        val solutionDigit: Int,                                
      44 +        val unitCells: Set<Int>,                               
      45 +        val currentCell: Int? = null,                          
      46 +        val eliminatorCells: Set<Int> = emptySet(),            
      47 +    ) : HintUi                                                 
      48  }

● Now GameReducer:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 77 lines
      230              )
      231      }
      232  
      233 +    // ---- Hidden-single hint animation ----                 
      234 +                                                              
      235 +    /**                                                       
      236 +     * Enter the hidden-single hint animation. Locks the phase
          +, paints the                                                  
      237 +     * solution digit as a pencil mark in every empty cell of 
          +the unit (so                                                  
      238 +     * the sweep has something visible to erase), and stashes 
          +the UI state.                                                 
      239 +     * Player-placed marks for other digits in those cells are
          + left alone.                                                  
      240 +     */                                                       
      241 +    fun beginHiddenSingleHint(state: GameState, hint: HintEngi
          +ne.HiddenSingle): GameState {                                 
      242 +        var s = state                                         
      243 +        for (cellIdx in hint.unit.cells) {                    
      244 +            val c = s.cells[cellIdx]                          
      245 +            if (c.value != null) continue                     
      246 +            if (hint.solutionDigit in c.pencilMarks) continue 
      247 +            s = s.replaceCell(c.copy(                         
      248 +                pencilMarks = c.pencilMarks + (hint.solutionDi
          +git to GuessColor.DEFAULT),                                   
      249 +            ))                                                
      250 +        }                                                     
      251 +        return s.copy(                                        
      252 +            phase = GameState.Phase.Hinting,                  
      253 +            activeDigit = null,                               
      254 +            hintUi = HintUi.HiddenSingle(                     
      255 +                targetCell = hint.targetCell,                 
      256 +                solutionDigit = hint.solutionDigit,           
      257 +                unitCells = hint.unit.cells.toSet(),          
      258 +            ),                                                
      259 +        )                                                     
      260 +    }                                                         
      261 +                                                              
      262 +    /**                                                       
      263 +     * Advance the unit sweep. When [currentCell] is non-null 
          +the solution                                                  
      264 +     * digit is erased from that cell's pencil marks; pass nul
          +l for the                                                     
      265 +     * final settle frame where only the target retains the di
          +git.                                                          
      266 +     */                                                       
      267 +    fun advanceHiddenSingleStep(                              
      268 +        state: GameState,                                     
      269 +        currentCell: Int?,                                    
      270 +        eliminatorCells: Set<Int>,                            
      271 +    ): GameState {                                            
      272 +        val ui = state.hintUi as? HintUi.HiddenSingle ?: retur
          +n state                                                       
      273 +        var s = state                                         
      274 +        if (currentCell != null) {                            
      275 +            val c = state.cells[currentCell]                  
      276 +            s = s.replaceCell(c.copy(pencilMarks = c.pencilMar
          +ks - ui.solutionDigit))                                       
      277 +        }                                                     
      278 +        return s.copy(hintUi = ui.copy(                       
      279 +            currentCell = currentCell,                        
      280 +            eliminatorCells = eliminatorCells,                
      281 +        ))                                                    
      282 +    }                                                         
      283 +                                                              
      284 +    /**                                                       
      285 +     * Finish the hint: place the solution digit at the target
          + in Blue,                                                     
      286 +     * tidy lingering solution-digit notes from the rest of th
          +e unit (they                                                  
      287 +     * are provably impossible there), clear hint UI, bump hin
          +tCount.                                                       
      288 +     */                                                       
      289 +    fun completeHiddenSingleHint(state: GameState, hint: HintE
          +ngine.HiddenSingle): GameState {                              
      290 +        val target = state.cells[hint.targetCell]             
      291 +        var s = state.replaceCell(target.copy(                
      292 +            value = hint.solutionDigit,                       
      293 +            valueColor = CellColor.Guess(GuessColor.Blue),    
      294 +            pencilMarks = emptyMap(),                         
      295 +        ))                                                    
      296 +        for (cellIdx in hint.unit.cells) {                    
      297 +            if (cellIdx == hint.targetCell) continue          
      298 +            val c = s.cells[cellIdx]                          
      299 +            if (c.value == null && hint.solutionDigit in c.pen
          +cilMarks) {                                                   
      300 +                s = s.replaceCell(c.copy(pencilMarks = c.penci
          +lMarks - hint.solutionDigit))                                 
      301 +            }                                                 
      302 +        }                                                     
      303 +        return s.copy(                                        
      304 +            phase = GameState.Phase.Playing,                  
      305 +            hintUi = null,                                    
      306 +            hintCount = state.hintCount + 1,                  
      307 +        )                                                     
      308 +    }                                                         
      309 +                                                              
      310      // ---- End-of-game actions ----
      311  
      312      fun beginAutoSolve(state: GameState): GameState =

● Now wire it into the ViewModel:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 1 line
      102                  }
      103              }
      104              is HintEngine.NakedSingle -> animateNakedSingle(hi
           nt)
      105 +            is HintEngine.HiddenSingle -> animateHiddenSingle(
          +hint)                                                         
      106          }
      107      }
      108  

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 28 lines
      127          }
      128      }
      129  
      130 +    private fun animateHiddenSingle(hint: HintEngine.HiddenSin
          +gle) {                                                        
      131 +        viewModelScope.launch {                               
      132 +            _state.value = GameReducer.beginHiddenSingleHint(_
          +state.value, hint)                                            
      133 +            delay(config.hintStepMs)                          
      134 +                                                              
      135 +            for (cellIdx in hint.unit.cells) {                
      136 +                if (cellIdx == hint.targetCell) continue      
      137 +                if (_state.value.cells[cellIdx].value != null)
          + continue                                                     
      138 +                _state.value = GameReducer.advanceHiddenSingle
          +Step(                                                         
      139 +                    state = _state.value,                     
      140 +                    currentCell = cellIdx,                    
      141 +                    eliminatorCells = hint.eliminators[cellIdx
          +].orEmpty(),                                                  
      142 +                )                                             
      143 +                delay(config.hintStepMs)                      
      144 +            }                                                 
      145 +                                                              
      146 +            // Settle frame: only the target retains the digit
          +.                                                             
      147 +            _state.value = GameReducer.advanceHiddenSingleStep
          +(                                                             
      148 +                state = _state.value,                         
      149 +                currentCell = null,                           
      150 +                eliminatorCells = emptySet(),                 
      151 +            )                                                 
      152 +            delay(config.hintStepMs)                          
      153 +                                                              
      154 +            _state.value = GameReducer.completeHiddenSingleHin
          +t(_state.value, hint)                                         
      155 +        }                                                     
      156 +    }                                                         
      157 +                                                              
      158      companion object {
      159          fun factory(repository: PuzzleRepository, config: Game
           Config, difficulty: Difficulty) =
      160              object : ViewModelProvider.Factory {

● Now Board.kt rendering:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 2 lines
      52      val highlight = Color(0xFFFFF59D) // pale yellow
      53      val targetBg = Color(0xFFA5D6A7) // hint target: green
      54      val eliminatorBg = Color(0xFFFFAB91) // hint eliminator pee
          r: light red
      55 +    val unitBg = Color(0xFFE3F2FD) // hint unit frame: very pal
         +e blue                                                         
      56 +    val currentBg = Color(0xFFFFE082) // hint sweep cursor: amb
         +er                                                             
      57      val pencilColor = Color(0xFF607D8B)
      58  
      59      Box(

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 38 lines
      144                          )
      145                      }
      146  
      147 +                    // Hidden-single overlay: unit frame, curr
          +ent cell,                                                     
      148 +                    // outside-unit eliminator peers, and targ
          +et. Drawn in                                                  
      149 +                    // order so the target stays on top.      
      150 +                    if (hint is HintUi.HiddenSingle) {        
      151 +                        for (idx in hint.unitCells) {         
      152 +                            val r = idx / 9; val c = idx % 9  
      153 +                            drawRect(                         
      154 +                                color = unitBg,               
      155 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      156 +                                size = Size(cellW, cellH),    
      157 +                            )                                 
      158 +                        }                                     
      159 +                        for (idx in hint.eliminatorCells) {   
      160 +                            val r = idx / 9; val c = idx % 9  
      161 +                            drawRect(                         
      162 +                                color = eliminatorBg,         
      163 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      164 +                                size = Size(cellW, cellH),    
      165 +                            )                                 
      166 +                        }                                     
      167 +                        val cur = hint.currentCell            
      168 +                        if (cur != null) {                    
      169 +                            val r = cur / 9; val c = cur % 9  
      170 +                            drawRect(                         
      171 +                                color = currentBg,            
      172 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      173 +                                size = Size(cellW, cellH),    
      174 +                            )                                 
      175 +                        }                                     
      176 +                        val tr = hint.targetCell / 9          
      177 +                        val tc = hint.targetCell % 9          
      178 +                        drawRect(                             
      179 +                            color = targetBg,                 
      180 +                            topLeft = Offset(tc * cellW, tr * 
          +cellH),                                                       
      181 +                            size = Size(cellW, cellH),        
      182 +                        )                                     
      183 +                    }                                         
      184 +                                                              
      185                      // Grid lines: thin for every cell, thick 
           for the 3x3 boxes.
      186                      for (i in 0..9) {
      187                          val isThick = i % 3 == 0

● Now add tests for the engine and the reducer flow.

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 42 lines
      48      }
      49  
      50      @Test
      51 +    fun `finds a hidden single in a box`() {                   
      52 +        // Block 5 from every cell of box 0 except (0,0) by pla
         +cing 5                                                         
      53 +        // along col 1, col 2, row 1, and row 2 (outside box 0)
         +.                                                              
      54 +        val grid = IntArray(81)                                
      55 +        grid[3 * 9 + 1] = 5                                    
      56 +        grid[4 * 9 + 2] = 5                                    
      57 +        grid[1 * 9 + 5] = 5                                    
      58 +        grid[2 * 9 + 7] = 5                                    
      59 +                                                               
      60 +        val hint = HintEngine.find(grid)                       
      61 +        assertNotNull(hint)                                    
      62 +        hint as HintEngine.HiddenSingle                        
      63 +        assertEquals(0, hint.targetCell)                       
      64 +        assertEquals(5, hint.solutionDigit)                    
      65 +        assertEquals("Hidden Single", hint.techniqueName)      
      66 +        assertEquals(HintEngine.UnitKind.Box, hint.unit.kind)  
      67 +        // Every other empty cell in the unit must report at le
         +ast one peer                                                   
      68 +        // outside the unit holding the digit.                 
      69 +        for (cellIdx in hint.unit.cells) {                     
      70 +            if (cellIdx == hint.targetCell) continue           
      71 +            if (grid[cellIdx] != 0) continue                   
      72 +            val peers = hint.eliminators[cellIdx]              
      73 +            assertNotNull(peers, "cell $cellIdx should have eli
         +minator peers")                                                
      74 +            assertTrue(peers!!.isNotEmpty())                   
      75 +            for (p in peers) assertEquals(5, grid[p])          
      76 +        }                                                      
      77 +    }                                                          
      78 +                                                               
      79 +    @Test                                                      
      80 +    fun `prefers naked single over hidden single when both appl
         +y`() {                                                         
      81 +        // (0,0) is a naked single (only 9 fits) AND simultaneo
         +usly a                                                         
      82 +        // hidden single for 9 in its row/column/box. Naked sho
         +uld win.                                                       
      83 +        val grid = IntArray(81)                                
      84 +        grid[1] = 1; grid[2] = 2; grid[3] = 3                  
      85 +        grid[4 * 9 + 0] = 4; grid[5 * 9 + 0] = 5               
      86 +        grid[1 * 9 + 1] = 6; grid[1 * 9 + 2] = 7; grid[2 * 9 + 
         +1] = 8                                                         
      87 +                                                               
      88 +        val hint = HintEngine.find(grid)                       
      89 +        assertTrue(hint is HintEngine.NakedSingle)             
      90 +    }                                                          
      91 +                                                               
      92 +    @Test                                                      
      93      fun `returns null on a fully solved grid`() {
      94          val solved = intArrayOf(
      95              5, 3, 4, 6, 7, 8, 9, 1, 2,

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 59 lines
      391      }
      392  
      393      @Test
      394 +    fun `hidden single hint flows from begin to complete`() { 
      395 +        // Construct a grid with a clean hidden single: (0,0) 
          +is the only                                                   
      396 +        // cell in box 0 that can hold 5, because 5 appears on
          + cols 1 & 2                                                   
      397 +        // and rows 1 & 2 outside the box.                    
      398 +        val grid = IntArray(81)                               
      399 +        grid[3 * 9 + 1] = 5                                   
      400 +        grid[4 * 9 + 2] = 5                                   
      401 +        grid[1 * 9 + 5] = 5                                   
      402 +        grid[2 * 9 + 7] = 5                                   
      403 +        val hint = HintEngine.find(grid) as HintEngine.HiddenS
          +ingle                                                         
      404 +        assertEquals(0, hint.targetCell)                      
      405 +        assertEquals(5, hint.solutionDigit)                   
      406 +                                                              
      407 +        // Build a state with this grid as the givens (we only
          + care about                                                   
      408 +        // value placement, not difficulty/solution alignment)
          +.                                                             
      409 +        val fakeSolution = IntArray(81) { i -> if (grid[i] != 
          +0) grid[i] else 1 }                                           
      410 +        fakeSolution[0] = 5                                   
      411 +        val fakePuzzle = Puzzle(                              
      412 +            id = "hs-test",                                   
      413 +            difficulty = Difficulty.Easy,                     
      414 +            givens = grid.toList(),                           
      415 +            solution = fakeSolution.toList(),                 
      416 +        )                                                     
      417 +        var s = GameState.forNewGame(fakePuzzle)              
      418 +                                                              
      419 +        s = GameReducer.beginHiddenSingleHint(s, hint)        
      420 +        assertEquals(GameState.Phase.Hinting, s.phase)        
      421 +        assertTrue(s.hintUi is HintUi.HiddenSingle)           
      422 +        // Every empty cell in the unit now carries the soluti
          +on digit as a mark.                                           
      423 +        for (idx in hint.unit.cells) {                        
      424 +            val cell = s.cells[idx]                           
      425 +            if (cell.value != null) continue                  
      426 +            assertEquals(GuessColor.DEFAULT, cell.pencilMarks[
          +5])                                                           
      427 +        }                                                     
      428 +                                                              
      429 +        // Sweep each non-target empty cell.                  
      430 +        for (idx in hint.unit.cells) {                        
      431 +            if (idx == hint.targetCell) continue              
      432 +            if (s.cells[idx].value != null) continue          
      433 +            s = GameReducer.advanceHiddenSingleStep(          
      434 +                state = s,                                    
      435 +                currentCell = idx,                            
      436 +                eliminatorCells = hint.eliminators[idx].orEmpt
          +y(),                                                          
      437 +            )                                                 
      438 +            assertNull(s.cells[idx].pencilMarks[5])           
      439 +        }                                                     
      440 +        // Settle frame: target still has the mark; everyone e
          +lse doesn't.                                                  
      441 +        s = GameReducer.advanceHiddenSingleStep(s, currentCell
          + = null, eliminatorCells = emptySet())                        
      442 +        assertEquals(GuessColor.DEFAULT, s.cells[hint.targetCe
          +ll].pencilMarks[5])                                           
      443 +                                                              
      444 +        s = GameReducer.completeHiddenSingleHint(s, hint)     
      445 +        assertEquals(GameState.Phase.Playing, s.phase)        
      446 +        assertNull(s.hintUi)                                  
      447 +        assertEquals(5, s.cells[hint.targetCell].value)       
      448 +        assertEquals(CellColor.Guess(GuessColor.Blue), s.cells
          +[hint.targetCell].valueColor)                                 
      449 +        assertEquals(1, s.hintCount)                          
      450 +    }                                                         
      451 +                                                              
      452 +    @Test                                                     
      453      fun `resetBoard returns to starting position`() {
      454          var s = initial()
      455          s = GameReducer.selectDigit(s, 4)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -40)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +16 lines (ctrl+o to expand)

● Domain tests green. Let me try to compile the app module too, in case the
  Android plugin lives in a local cache.

● Bash(./gradlew :app:compileDebugKotlin --no-daemon --offline 2>&1 | tail -30)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +23 lines (ctrl+o to expand)

● Can't build the app module in this environment (Google Maven blocked). Let me
  re-read the app-layer edits carefully to catch any compile issue.

  Read 2 files (ctrl+o to expand)
● Bash(./gradlew :domain:test :tools:puzzle-gen:test -PincludeApp=false
      --no-daemon 2>&1 | tail -15)
  ⎿  > Task :domain:compileTestJava NO-SOURCE
     > Task :domain:processTestResources NO-SOURCE
     > Task :domain:testClasses UP-TO-DATE
     … +12 lines (ctrl+o to expand)

● Bash(git status && echo "---" && git diff --stat)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Your branch is up to date with 'origin/claude/add-sudoku-hint-button-L
     1spT'.
     … +21 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Hidden Single hint with unit-sweep teaching animation…)
  ⎿  [claude/add-sudoku-hint-button-L1spT d65b4a6] Add Hidden Single hint w
     ith unit-sweep teaching animation
      7 files changed, 378 insertions(+)

● Bash(git push -u origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -10)
  ⎿  To http://127.0.0.1:35449/git/dividetask/soduku-trainer
      ! [rejected]        claude/add-sudoku-hint-button-L1spT -> claude/add
     -sudoku-hint-button-L1spT (fetch first)
     … +10 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT && git log
      origin/claude/add-sudoku-hint-button-L1spT --oneline -5)
  ⎿  From http://127.0.0.1:35449/git/dividetask/soduku-trainer
      * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        0c55f81..4528132  claude/add-sudoku-hint-button-L1spT -> origin/cla
     … +6 lines (ctrl+o to expand)

● Bash(git log origin/claude/add-sudoku-hint-button-L1spT --stat -2 2>&1 | head
      -50)
  ⎿  commit 452813276dc30ce523cec5f7707553c8e8c4dbfb
     Author: Brett <info@binarybills.com>
     Date:   Tue May 19 11:50:47 2026 -0700
     … +15 lines (ctrl+o to expand)

● User pushed version bumps while I worked. Rebasing onto remote tip.

● Bash(git rebase origin/claude/add-sudoku-hint-button-L1spT 2>&1 | tail -15)
  ⎿  Rebasing (1/1)Successfully rebased and updated refs/heads/claude/add-s
     udoku-hint-button-L1spT.

● Bash(git push -u origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -10)
  ⎿  To http://127.0.0.1:35449/git/dividetask/soduku-trainer
        4528132..85f27c8  claude/add-sudoku-hint-button-L1spT -> claude/add
     -sudoku-hint-button-L1spT
     … +2 lines (ctrl+o to expand)

● Pushed 85f27c8. Summary of what landed:

  Engine (HintEngine.kt)
  - New HintEngine.HiddenSingle hint variant alongside NakedSingle, plus
  HintUnit { kind, cells } and UnitKind { Box, Row, Column }.
  - findHiddenSingle iterates units in box → row → column order and, for each
  digit not yet placed in the unit, finds the unique legal cell. Eliminators map
  each other empty cell in the unit to the peers outside the unit that block
  the digit there. Filled cells in the unit need no entry — self-evident.
  - find() tries naked singles first; verified by a "prefers naked over hidden"
  test.

  Reducer (GameReducer.kt)
  - beginHiddenSingleHint: writes the solution digit as a default-color pencil
  mark into every empty cell of the unit (skipping cells that already have it).
  Player marks for other digits are preserved.
  - advanceHiddenSingleStep(currentCell, eliminatorCells): erases the solution
  digit from currentCell's notes; null means the final settle frame.
  - completeHiddenSingleHint: places the digit in Blue at the target, tidies any
  leftover solution-digit notes from the rest of the unit, bumps hintCount.

  View model — adds animateHiddenSingle: begin, sweep each non-target empty cell
  in unit order with hintStepMs between frames, one settle frame, complete.

  Board — new pale-blue unit-frame tint, amber current-cell cursor; existing red
  eliminators and green target colors reused. Target is drawn last so it stays
  visible inside the framed unit.

  Tests — domain tests pass (./gradlew :domain:test :tools:puzzle-gen:test 
  -PincludeApp=false). I couldn't compile :app here (Google Maven blocked in
  this sandbox), so the new Kotlin in GameViewModel and Board is unverified by
  the compiler — worth a quick :app:compileDebugKotlin on your machine.

❯ This might happen in subprojects that apply the Kotlin plugins with the       
  Gradle 'plugins { ... }' DSL if they specify explicit versions, even if the   
  versions are equal.                                                           
  Please add the Kotlin plugin to the common parent project or the root         
  project, then remove the versions in the subprojects.                         
  If the parent project does not need the plugin, add 'apply false' to the      
  plugin line.                                                                  
  See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_p 
  lugins_dsl                                                                    
  The Kotlin plugin was loaded in the following projects: ':app', ':domain'     
                                                                                
  > Task :app:compileDebugKotlin FAILED                                         
  e: file:///home/brett/Projects/soduku-trainer/app/src/main/kotlin/com/dividet 
  ask/sudokutrainer/ui/game/GameScreen.kt:90:16 'when' expression must be       
  exhaustive. Add the 'is HiddenSingle' branch or an 'else' branch.             
  w: Detected multiple Kotlin daemon sessions at                                
                                                                                
  FAILURE: Build failed with an exception.                                      
                                                                                
  * What went wrong:                                                            
  Execution failed for task ':app:compileDebugKotlin'.                          
  > A failure occurred while executing org.jetbrains.kotlin.compilerRunner.Grad 
  leCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction                    
     > Compilation error. See log for more details                              
                                                                                
  * Try:                                                                        
  > Run with --stacktrace option to get the stack trace.                        
  > Run with --info or --debug option to get more log output.                   
  > Run with --scan to get full insights.                                       
  > Get more help at https://help.gradle.org.                                   
                                                                                
  BUILD FAILED in 45s                                                           
  30 actionable tasks: 4 executed, 26 up-to-date                                

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      89  private fun HintLabel(state: GameState) {
      90      val text = when (state.hintUi) {
      91          is HintUi.NakedSingle -> "Naked Single"
      92 +        is HintUi.HiddenSingle -> "Hidden Single"              
      93          HintUi.NoHintFlash -> "No hint available"
      94          null -> ""
      95      }

● Bash(git add -A && git commit -m "Add Hidden Single branch to HintLabel" &&
      git push 2>&1 | tail -5)
  ⎿  [claude/add-sudoku-hint-button-L1spT d744e49] Add Hidden Single branch
      to HintLabel
      1 file changed, 1 insertion(+)
     … +3 lines (ctrl+o to expand)

● Fixed and pushed d744e49 — added the is HintUi.HiddenSingle -> "Hidden Single"
  branch to the exhaustive when in GameScreen.HintLabel.

❯ The undo button should undo moves made via a hint                             

  Read 1 file (ctrl+o to expand)

● I'll make the hint completion record a Move.SetValue so undo reverses it (and
  also restores any pre-hint notes that the animation overwrote). For Hidden
  Single, undo will also restore the solution-digit marks the sweep erased from
  other unit cells.

  To do that cleanly, the begin step needs to snapshot the pre-hint notes
  (target cell + any solution-digit marks the player had elsewhere in the unit).
  I'll stash that on the HintUi so complete can build the move.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Added 16 lines
      20       *   evaluated and [eliminatorCells] are the peer cells tha
          t prove that
      21       *   digit can't go in the target (or empty for the solutio
          n digit).
      22       */
      23 +    /**                                                        
      24 +     * Naked-single teaching animation in progress.            
      25 +     *                                                         
      26 +     * - [targetCell] is shaded green for the duration.        
      27 +     * - During the 1..9 sweep, [activeDigit] is the digit curr
         +ently being                                                    
      28 +     *   evaluated and [eliminatorCells] are the peer cells tha
         +t prove that                                                   
      29 +     *   digit can't go in the target (or empty for the solutio
         +n digit).                                                      
      30 +     * - [originalNotes] captures the target cell's pencil mark
         +s at begin                                                     
      31 +     *   (which are then overwritten by the 1..9 sweep) so undo
         + can                                                           
      32 +     *   restore them when the hint move is reversed.          
      33 +     */                                                        
      34      data class NakedSingle(
      35          val targetCell: Int,
      36          val solutionDigit: Int,
      37          val activeDigit: Int? = null,
      38          val eliminatorCells: Set<Int> = emptySet(),
      39 +        val originalNotes: Map<Int, GuessColor> = emptyMap(),  
      40      ) : HintUi
      41  
      42      /**
     ...
      49       *   elimination is currently being shown, and [eliminatorC
          ells] are
      50       *   the peer cells *outside* the unit that prove the solut
          ion digit
      51       *   can't go there.
      52 +     * - [targetOriginalNotes] and [clearedPeerMarks] capture p
         +re-hint                                                        
      53 +     *   state so undo can restore notes the animation erased. 
      54       */
      55      data class HiddenSingle(
      56          val targetCell: Int,
     ...
      58          val unitCells: Set<Int>,
      59          val currentCell: Int? = null,
      60          val eliminatorCells: Set<Int> = emptySet(),
      61 +        val targetOriginalNotes: Map<Int, GuessColor> = emptyMa
         +p(),                                                           
      62 +        val clearedPeerMarks: List<AutoClearedMark> = emptyList
         +(),                                                            
      63      ) : HintUi
      64  }

  Read 1 file (ctrl+o to expand)

● Duplicate doc block — fix:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Removed 8 lines
      19       * - During the 1..9 sweep, [activeDigit] is the digit curr
          ently being
      20       *   evaluated and [eliminatorCells] are the peer cells tha
          t prove that
      21       *   digit can't go in the target (or empty for the solutio
          n digit).
      22 -     */                                                        
      23 -    /**                                                        
      24 -     * Naked-single teaching animation in progress.            
      25 -     *                                                         
      26 -     * - [targetCell] is shaded green for the duration.        
      27 -     * - During the 1..9 sweep, [activeDigit] is the digit curr
         -ently being                                                    
      28 -     *   evaluated and [eliminatorCells] are the peer cells tha
         -t prove that                                                   
      29 -     *   digit can't go in the target (or empty for the solutio
         -n digit).                                                      
      22       * - [originalNotes] captures the target cell's pencil mark
          s at begin
      23       *   (which are then overwritten by the 1..9 sweep) so undo
           can
      24       *   restore them when the hint move is reversed.
● Now the reducer — capture pre-hint state in begin, emit a Move.SetValue in
  complete:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 2 lines
      173       */
      174      fun beginNakedSingleHint(state: GameState, hint: HintEngin
           e.NakedSingle): GameState {
      175          val target = state.cells[hint.targetCell]
      176 +        val originalNotes = target.pencilMarks                
      177          val notes = (1..9).associateWith { GuessColor.DEFAULT 
           }
      178          return state
      179              .replaceCell(target.copy(pencilMarks = notes))
     ...
      183                  hintUi = HintUi.NakedSingle(
      184                      targetCell = hint.targetCell,
      185                      solutionDigit = hint.solutionDigit,
      186 +                    originalNotes = originalNotes,            
      187                  ),
      188              )
      189      }

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 11 lines
      215      /**
      216       * Finish the hint: place the solution digit in Blue (Solv
           e color),
      217       * clear notes, clear hint UI, bump the hint count, return
            to Playing.
      218 +     * Records a [Move.SetValue] so undo reverses both the pla
          +cement and                                                    
      219 +     * any pencil marks the animation overwrote in the target 
          +cell.                                                         
      220       */
      221      fun completeNakedSingleHint(state: GameState, hint: HintEn
           gine.NakedSingle): GameState {
      222 +        val ui = state.hintUi as? HintUi.NakedSingle          
      223          val target = state.cells[hint.targetCell]
      224          val placed = target.copy(
      225              value = hint.solutionDigit,
      226              valueColor = CellColor.Guess(GuessColor.Blue),
      227              pencilMarks = emptyMap(),
      228          )
      229 +        val move = Move.SetValue(                             
      230 +            row = target.row,                                 
      231 +            col = target.col,                                 
      232 +            newValue = hint.solutionDigit,                    
      233 +            newValueColor = GuessColor.Blue,                  
      234 +            clearedMarks = ui?.originalNotes.orEmpty(),       
      235 +        )                                                     
      236          return state
      237              .replaceCell(placed)
      238              .copy(
      239                  phase = GameState.Phase.Playing,
      240                  hintUi = null,
      241                  hintCount = state.hintCount + 1,
      242 +                history = state.history + move,               
      243              )
      244      }
      245  

● Now the same for HiddenSingle — capture solution-digit notes the player had in
  other unit cells:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 16 lines
      252       * Player-placed marks for other digits in those cells are
            left alone.
      253       */
      254      fun beginHiddenSingleHint(state: GameState, hint: HintEngi
           ne.HiddenSingle): GameState {
      255 +        val target = state.cells[hint.targetCell]             
      256 +        val targetOriginalNotes = target.pencilMarks          
      257 +        val clearedPeerMarks = mutableListOf<AutoClearedMark>(
          +)                                                             
      258 +        for (cellIdx in hint.unit.cells) {                    
      259 +            if (cellIdx == hint.targetCell) continue          
      260 +            val c = state.cells[cellIdx]                      
      261 +            if (c.value != null) continue                     
      262 +            val existingColor = c.pencilMarks[hint.solutionDig
          +it]                                                           
      263 +            if (existingColor != null) {                      
      264 +                clearedPeerMarks += AutoClearedMark(          
      265 +                    row = c.row, col = c.col, digit = hint.sol
          +utionDigit, color = existingColor,                            
      266 +                )                                             
      267 +            }                                                 
      268 +        }                                                     
      269          var s = state
      270          for (cellIdx in hint.unit.cells) {
      271              val c = s.cells[cellIdx]
     ...
      282                  targetCell = hint.targetCell,
      283                  solutionDigit = hint.solutionDigit,
      284                  unitCells = hint.unit.cells.toSet(),
      285 +                targetOriginalNotes = targetOriginalNotes,    
      286 +                clearedPeerMarks = clearedPeerMarks,          
      287              ),
      288          )
      289      }

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 12 lines
      314       * Finish the hint: place the solution digit at the target
            in Blue,
      315       * tidy lingering solution-digit notes from the rest of th
           e unit (they
      316       * are provably impossible there), clear hint UI, bump hin
           tCount.
      317 +     * Records a [Move.SetValue] whose autoClearedMarks restor
          +e any                                                         
      318 +     * player-placed solution-digit notes that the sweep erase
          +d.                                                            
      319       */
      320      fun completeHiddenSingleHint(state: GameState, hint: HintE
           ngine.HiddenSingle): GameState {
      321 +        val ui = state.hintUi as? HintUi.HiddenSingle         
      322          val target = state.cells[hint.targetCell]
      323          var s = state.replaceCell(target.copy(
      324              value = hint.solutionDigit,
     ...
      332                  s = s.replaceCell(c.copy(pencilMarks = c.penci
           lMarks - hint.solutionDigit))
      333              }
      334          }
      335 +        val move = Move.SetValue(                             
      336 +            row = target.row,                                 
      337 +            col = target.col,                                 
      338 +            newValue = hint.solutionDigit,                    
      339 +            newValueColor = GuessColor.Blue,                  
      340 +            clearedMarks = ui?.targetOriginalNotes.orEmpty(), 
      341 +            autoClearedMarks = ui?.clearedPeerMarks.orEmpty(),
      342 +        )                                                     
      343          return s.copy(
      344              phase = GameState.Phase.Playing,
      345              hintUi = null,
      346              hintCount = state.hintCount + 1,
      347 +            history = state.history + move,                   
      348          )
      349      }
      350  

● Add tests for the new undo behavior:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 94 lines
      450      }
      451  
      452      @Test
      453 +    fun `undo reverses a naked single hint and restores prior 
          +pencil marks`() {                                             
      454 +        var s = initial()                                     
      455 +        // Seed pencil marks in the cell where the puzzle has 
          +a naked single.                                               
      456 +        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      457 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      458 +        val tRow = hint.targetCell / 9                        
      459 +        val tCol = hint.targetCell % 9                        
      460 +        s = GameReducer.togglePencil(s)                       
      461 +        s = GameReducer.selectColor(s, GuessColor.Red)        
      462 +        s = GameReducer.selectDigit(s, 2)                     
      463 +        s = GameReducer.tapCell(s, tRow, tCol)                
      464 +        s = GameReducer.selectDigit(s, 7)                     
      465 +        s = GameReducer.tapCell(s, tRow, tCol)                
      466 +        s = GameReducer.togglePencil(s)                       
      467 +        val historySizeBeforeHint = s.history.size            
      468 +        assertEquals(setOf(2, 7), s.cellAt(tRow, tCol).pencilM
          +arks.keys)                                                    
      469 +                                                              
      470 +        // Run the hint to completion.                        
      471 +        s = GameReducer.beginNakedSingleHint(s, hint)         
      472 +        for (d in 1..9) {                                     
      473 +            s = GameReducer.advanceNakedSingleStep(           
      474 +                state = s,                                    
      475 +                activeDigit = d,                              
      476 +                eliminatorCells = hint.eliminators[d].orEmpty(
          +),                                                            
      477 +                eliminate = d != hint.solutionDigit,          
      478 +            )                                                 
      479 +        }                                                     
      480 +        s = GameReducer.completeNakedSingleHint(s, hint)      
      481 +        assertEquals(hint.solutionDigit, s.cellAt(tRow, tCol).
          +value)                                                        
      482 +        assertEquals(historySizeBeforeHint + 1, s.history.size
          +)                                                             
      483 +        assertTrue(s.history.last() is Move.SetValue)         
      484 +                                                              
      485 +        // Undo: value gone, prior pencil marks restored.     
      486 +        s = GameReducer.undo(s)                               
      487 +        assertNull(s.cellAt(tRow, tCol).value)                
      488 +        assertEquals(GuessColor.Red, s.cellAt(tRow, tCol).penc
          +ilMarks[2])                                                   
      489 +        assertEquals(GuessColor.Red, s.cellAt(tRow, tCol).penc
          +ilMarks[7])                                                   
      490 +        assertEquals(historySizeBeforeHint, s.history.size)   
      491 +    }                                                         
      492 +                                                              
      493 +    @Test                                                     
      494 +    fun `undo reverses a hidden single hint and restores sweep
          +-erased peer marks`() {                                       
      495 +        // Same hidden-single setup as the begin-to-complete t
          +est.                                                          
      496 +        val grid = IntArray(81)                               
      497 +        grid[3 * 9 + 1] = 5                                   
      498 +        grid[4 * 9 + 2] = 5                                   
      499 +        grid[1 * 9 + 5] = 5                                   
      500 +        grid[2 * 9 + 7] = 5                                   
      501 +        val hint = HintEngine.find(grid) as HintEngine.HiddenS
          +ingle                                                         
      502 +        val fakeSolution = IntArray(81) { i -> if (grid[i] != 
          +0) grid[i] else 1 }                                           
      503 +        fakeSolution[0] = 5                                   
      504 +        val fakePuzzle = Puzzle(                              
      505 +            id = "hs-undo-test",                              
      506 +            difficulty = Difficulty.Easy,                     
      507 +            givens = grid.toList(),                           
      508 +            solution = fakeSolution.toList(),                 
      509 +        )                                                     
      510 +        var s = GameState.forNewGame(fakePuzzle)              
      511 +                                                              
      512 +        // Player has placed a Purple 5 pencil mark in (0,1) —
          + a non-target                                                 
      513 +        // cell in box 0 that the sweep will erase.           
      514 +        s = GameReducer.togglePencil(s)                       
      515 +        s = GameReducer.selectColor(s, GuessColor.Purple)     
      516 +        s = GameReducer.selectDigit(s, 5)                     
      517 +        s = GameReducer.tapCell(s, 0, 1)                      
      518 +        s = GameReducer.togglePencil(s)                       
      519 +        assertEquals(GuessColor.Purple, s.cellAt(0, 1).pencilM
          +arks[5])                                                      
      520 +        val historySizeBeforeHint = s.history.size            
      521 +                                                              
      522 +        // Run the hidden single hint to completion.          
      523 +        s = GameReducer.beginHiddenSingleHint(s, hint)        
      524 +        for (idx in hint.unit.cells) {                        
      525 +            if (idx == hint.targetCell) continue              
      526 +            if (s.cells[idx].value != null) continue          
      527 +            s = GameReducer.advanceHiddenSingleStep(          
      528 +                state = s,                                    
      529 +                currentCell = idx,                            
      530 +                eliminatorCells = hint.eliminators[idx].orEmpt
          +y(),                                                          
      531 +            )                                                 
      532 +        }                                                     
      533 +        s = GameReducer.advanceHiddenSingleStep(s, currentCell
          + = null, eliminatorCells = emptySet())                        
      534 +        s = GameReducer.completeHiddenSingleHint(s, hint)     
      535 +        assertEquals(5, s.cellAt(0, 0).value)                 
      536 +        assertNull(s.cellAt(0, 1).pencilMarks[5])             
      537 +        assertEquals(historySizeBeforeHint + 1, s.history.size
          +)                                                             
      538 +                                                              
      539 +        // Undo: value gone, player's Purple 5 mark restored o
          +n (0,1).                                                      
      540 +        s = GameReducer.undo(s)                               
      541 +        assertNull(s.cellAt(0, 0).value)                      
      542 +        assertEquals(GuessColor.Purple, s.cellAt(0, 1).pencilM
          +arks[5])                                                      
      543 +        assertEquals(historySizeBeforeHint, s.history.size)   
      544 +    }                                                         
      545 +                                                              
      546 +    @Test                                                     
      547      fun `resetBoard returns to starting position`() {
      548          var s = initial()
      549          s = GameReducer.selectDigit(s, 4)

● Also update the existing naked single hint flows... test which asserts on
  history-related state — it should still pass, but let me verify by running:

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -25)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +22 lines (ctrl+o to expand)

● Tests pass. Commit and push.

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Record hint placements in history so undo reverses them…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 845f78f] Record hint placements i
     n history so undo reverses them
      3 files changed, 143 insertions(+)
     … +8 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT && git log --oneline
      origin/claude/add-sudoku-hint-button-L1spT -5)
  ⎿  From http://127.0.0.1:36803/git/dividetask/soduku-trainer
      * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        d744e49..ee1ae58  claude/add-sudoku-hint-button-L1spT -> origin/cla
     … +6 lines (ctrl+o to expand)

● Bash(git rebase origin/claude/add-sudoku-hint-button-L1spT 2>&1 | tail -5 &&
      git push 2>&1 | tail -5)
  ⎿  Rebasing (1/1)Successfully rebased and updated refs/heads/claude/add-s
     udoku-hint-button-L1spT.
     To http://127.0.0.1:36803/git/dividetask/soduku-trainer
     … +2 lines (ctrl+o to expand)

● Pushed 9997bf5.

  What changed:
  - beginNakedSingleHint captures the target cell's pre-hint pencil marks
  (the 1..9 overlay would otherwise destroy them).
  - beginHiddenSingleHint additionally captures every player-placed
  solution-digit mark elsewhere in the unit (the sweep erases those).
  - completeNakedSingleHint / completeHiddenSingleHint now append a
  Move.SetValue with clearedMarks (target cell's pre-hint notes) and
  autoClearedMarks (peer notes the sweep erased, HiddenSingle only).

  The existing GameReducer.undo already knows how to reverse Move.SetValue —
  restore peer auto-cleared marks, then restore the target cell's
  value=null/notes — so undo of a hint move is a single press that fully
  restores pre-hint state.

  Two new tests cover it: undo after Naked Single restores the player's pencil
  marks in the target cell; undo after Hidden Single restores a Purple 5 mark
  the sweep erased from a non-target unit cell.

❯ Please allow the hint button to be pushed while it is doing the hint          
  animation. Pushing the button during the animation will cause the animation   
  to skip to filling in the answer.                                             
  Please also create another option under extreme called test. The YAML file    
  will have a boolean flag for showing or hiding this button but by default the 
  button will be visible. Clicking test will show each hint animation in order  
  from newest to oldest, it will generate a puzzle that has a cell that can be  
  solved by a specific strategy covered by our hints then display the           
  animation for it so I can see if I like it.                                   

  Searched for 1 pattern, read 7 files, listed 1 directory (ctrl+o to expand)

● Plan in two parts:

  Part 1 — Interruptible hint button. Extract a HintAnimator that owns the
  running animation and a skip() method that cancels the coroutine and applies
  completeXSingleHint immediately. Update the hint button to call skip when the
  phase is Hinting.

  Part 2 — Test menu. Add a show_test_button flag to GameConfig (default true),
  a "Test" button below the difficulties, a new nav route, and a TestViewModel /
  TestScreen that walks the technique list newest→oldest. Each case uses a
  hand-crafted grid completed by SudokuSolver.solveOne.

  Let me check whether the menu list looks fine with one more button and confirm
  the technique ordering.

  Read 1 file (ctrl+o to expand)

● Let me start with Part 1 — extract HintAnimator and rewire GameViewModel.

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/game/HintAnimator.kt)
  ⎿  Wrote 105 lines to ../../../user/soduku-trainer/app/src/main/kotlin/co
     m/dividetask/sudokutrainer/ui/game/HintAnimator.kt
      1 package com.dividetask.sudokutrainer.ui.game
      2 
      3 import com.dividetask.sudokutrainer.domain.GameReducer
      4 import com.dividetask.sudokutrainer.domain.GameState
      5 import com.dividetask.sudokutrainer.domain.HintEngine
      6 import kotlinx.coroutines.CoroutineScope
      7 import kotlinx.coroutines.Job
      8 import kotlinx.coroutines.delay
      9 import kotlinx.coroutines.launch
     … +111 lines (ctrl+o to expand)

● Now refactor GameViewModel to use HintAnimator + skip-during-animation:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/game/GameViewModel.kt)
  ⎿  Added 20 lines, removed 58 lines
      32      private val _exitSignal = MutableStateFlow(false)
      33      val exitSignal: StateFlow<Boolean> = _exitSignal.asStateFlo
          w()
      34  
      35 +    private val animator = HintAnimator(                       
      36 +        scope = viewModelScope,                                
      37 +        stepDelayMs = { config.hintStepMs },                   
      38 +        getState = { _state.value },                           
      39 +        setState = { _state.value = it },                      
      40 +    )                                                          
      41 +                                                               
      42      /** Whether the puzzle qualifies for auto-solve (driven by 
          config.yaml). */
      43      val isReadyToSolve: Boolean
      44          get() = _state.value.isMostlySolved(config.solveCriteri
          a)
     ...
       92  
       93      fun onHint() {
       94          val s = _state.value
       95 +        // Pressing Hint while an animation is in flight skips
          + to the                                                       
       96 +        // final placement.                                   
       97 +        if (animator.isRunning) {                             
       98 +            animator.skip()                                   
       99 +            return                                            
      100 +        }                                                     
      101          if (s.phase != GameState.Phase.Playing) return
      102          if (s.hintUi != null) return
      103  
     ...
      106          val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      107          val hint = if (s.hasIncorrectEntry) null else HintEngi
           ne.find(grid)
      108  
       96 -        when (hint) {                                         
       97 -            null -> {                                         
       98 -                _state.value = s.copy(hintUi = HintUi.NoHintFl
          -ash)                                                          
       99 -                viewModelScope.launch {                       
      100 -                    delay(config.hintFlashMs)                 
      101 -                    _state.value = _state.value.copy(hintUi = 
          -null)                                                         
      102 -                }                                             
      109 +        if (hint == null) {                                   
      110 +            _state.value = s.copy(hintUi = HintUi.NoHintFlash)
      111 +            viewModelScope.launch {                           
      112 +                delay(config.hintFlashMs)                     
      113 +                _state.value = _state.value.copy(hintUi = null
          +)                                                             
      114              }
      104 -            is HintEngine.NakedSingle -> animateNakedSingle(hi
          -nt)                                                           
      105 -            is HintEngine.HiddenSingle -> animateHiddenSingle(
          -hint)                                                         
      115 +            return                                            
      116          }
      117 +        animator.launch(hint)                                 
      118      }
      119  
      109 -    private fun animateNakedSingle(hint: HintEngine.NakedSingl
          -e) {                                                          
      110 -        viewModelScope.launch {                               
      111 -            // Lock the board, set up the visual: green target
          -, all 1..9 notes.                                             
      112 -            _state.value = GameReducer.beginNakedSingleHint(_s
          -tate.value, hint)                                             
      113 -            delay(config.hintStepMs)                          
      114 -                                                              
      115 -            for (d in 1..9) {                                 
      116 -                val eliminators = hint.eliminators[d].orEmpty(
          -)                                                             
      117 -                _state.value = GameReducer.advanceNakedSingleS
          -tep(                                                          
      118 -                    _state.value,                             
      119 -                    activeDigit = d,                          
      120 -                    eliminatorCells = eliminators,            
      121 -                    eliminate = d != hint.solutionDigit,      
      122 -                )                                             
      123 -                delay(config.hintStepMs)                      
      124 -            }                                                 
      125 -                                                              
      126 -            _state.value = GameReducer.completeNakedSingleHint
          -(_state.value, hint)                                          
      127 -        }                                                     
      128 -    }                                                         
      129 -                                                              
      130 -    private fun animateHiddenSingle(hint: HintEngine.HiddenSin
          -gle) {                                                        
      131 -        viewModelScope.launch {                               
      132 -            _state.value = GameReducer.beginHiddenSingleHint(_
          -state.value, hint)                                            
      133 -            delay(config.hintStepMs)                          
      134 -                                                              
      135 -            for (cellIdx in hint.unit.cells) {                
      136 -                if (cellIdx == hint.targetCell) continue      
      137 -                if (_state.value.cells[cellIdx].value != null)
          - continue                                                     
      138 -                _state.value = GameReducer.advanceHiddenSingle
          -Step(                                                         
      139 -                    state = _state.value,                     
      140 -                    currentCell = cellIdx,                    
      141 -                    eliminatorCells = hint.eliminators[cellIdx
          -].orEmpty(),                                                  
      142 -                )                                             
      143 -                delay(config.hintStepMs)                      
      144 -            }                                                 
      145 -                                                              
      146 -            // Settle frame: only the target retains the digit
          -.                                                             
      147 -            _state.value = GameReducer.advanceHiddenSingleStep
          -(                                                             
      148 -                state = _state.value,                         
      149 -                currentCell = null,                           
      150 -                eliminatorCells = emptySet(),                 
      151 -            )                                                 
      152 -            delay(config.hintStepMs)                          
      153 -                                                              
      154 -            _state.value = GameReducer.completeHiddenSingleHin
          -t(_state.value, hint)                                         
      155 -        }                                                     
      156 -    }                                                         
      157 -                                                              
      120      companion object {
      121          fun factory(repository: PuzzleRepository, config: Game
           Config, difficulty: Difficulty) =
      122              object : ViewModelProvider.Factory {

● Now enable the hint button during the Hinting phase so the user can skip:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 6 lines, removed 2 lines
      183  ) {
      184      val canUndo = state.history.isNotEmpty() && state.phase ==
            GameState.Phase.Playing
      185      val busy = state.phase != GameState.Phase.Playing
      186 +    // The hint button stays enabled while a hint is animating
          + so pressing                                                  
      187 +    // it again skips the animation to the final placement.   
      188 +    val canHint = state.phase == GameState.Phase.Playing ||   
      189 +        state.phase == GameState.Phase.Hinting                
      190  
      191      Row(
      192          modifier = modifier.fillMaxWidth(),
     ...
      201          }
      202          Button(
      203              onClick = onHint,
      200 -            enabled = !busy,                                  
      204 +            enabled = canHint,                                
      205              modifier = Modifier.weight(1f),
      206              colors = ButtonDefaults.buttonColors(
      207                  containerColor = Color(0xFF2E7D32),
      208              ),
      209          ) {
      206 -            Text("Hint")                                      
      210 +            Text(if (state.phase == GameState.Phase.Hinting) "
          +Skip" else "Hint")                                            
      211          }
      212          Button(
      213              onClick = onClear,
● Now Part 2 — config flag, test cases, view model, screen, nav, menu button.

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
       9      val fireworkDuration: Long = 120_000,
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12 +    val showTestButton: Boolean = true,                        
      13  ) {
      14      companion object {
      15          fun loadFromAssets(context: Context): GameConfig {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      35                  fireworkDuration = map["firework_duration"]?.to
          LongOrNull() ?: 120_000,
      36                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      37                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      38 +                showTestButton = map["show_test_button"]?.toBoo
         +leanStrictOrNull() ?: true,                                    
      39              )
      40          }
      41      }

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 5 lines
      24  # hint_flash_ms: Duration of the red board flash shown when the
           hint
      25  #                engine can't find an applicable technique.
      26  hint_flash_ms: 600
      27 +                                                               
      28 +# show_test_button: When true, the main menu shows a "Test" but
         +ton below                                                      
      29 +#                   Extreme that walks through each hint techni
         +que's                                                          
      30 +#                   teaching animation on a pre-built example p
         +uzzle.                                                         
      31 +show_test_button: true                                         

● Now the HintTestCases source (hand-built grids + their completions) and
  TestViewModel:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/HintTestCases.kt)
  ⎿  Wrote 66 lines to ../../../user/soduku-trainer/app/src/main/kotlin/com
     /dividetask/sudokutrainer/ui/test/HintTestCases.kt
       2 package com.dividetask.sudokutrainer.ui.test
       3 import com.dividetask.sudokutrainer.domain.Difficulty
       4 import com.dividetask.sudokutrainer.domain.Puzzle
       5 import com.dividetask.sudokutrainer.domain.SudokuSolver
       6 
       8  * Pre-built puzzles whose next hint is guaranteed to be a speci
         fic
       9  * technique. Used by the "Test" menu to preview each teaching a
         nimation
      10  * in turn.
     … +58 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/HintTestCases.kt)
  ⎿  Added 5 lines, removed 16 lines
       7  /**
       8   * Pre-built puzzles whose next hint is guaranteed to be a spec
          ific
       9   * technique. Used by the "Test" menu to preview each teaching 
          animation
      10 - * in turn.                                                    
      11 - *                                                             
      12 - * Ordering is newest-technique-first: as we add level-3+ techn
         -iques                                                          
      13 - * (naked pair, hidden pair, …) they prepend here.             
      10 + * in turn. Ordering is newest-technique-first.                
      11   */
      12  object HintTestCases {
      13  
     ...
      15  
      16      val cases: List<Case> by lazy {
      17          listOf(
      21 -            build(                                             
      22 -                title = "Hidden Single",                       
      23 -                id = "test-hidden-single",                     
      24 -                givens = intArrayOf(81).hiddenSingleGivens(),  
      25 -            ),                                                 
      26 -            build(                                             
      27 -                title = "Naked Single",                        
      28 -                id = "test-naked-single",                      
      29 -                givens = intArrayOf(81).nakedSingleGivens(),   
      30 -            ),                                                 
      18 +            build("Hidden Single", "test-hidden-single", hidden
         +SingleGivens()),                                               
      19 +            build("Naked Single", "test-naked-single", nakedSin
         +gleGivens()),                                                  
      20          )
      21      }
      22  
      23      /** (0,0) is a hidden single for 5 in box 0. */
      35 -    private fun IntArray.hiddenSingleGivens(): IntArray {      
      24 +    private fun hiddenSingleGivens(): IntArray {               
      25          val g = IntArray(81)
      26          g[3 * 9 + 1] = 5
      27          g[4 * 9 + 2] = 5
     ...
      31      }
      32  
      33      /** (0,0) is a naked single (only 9 fits). */
      45 -    private fun IntArray.nakedSingleGivens(): IntArray {       
      34 +    private fun nakedSingleGivens(): IntArray {                
      35          val g = IntArray(81)
      36          g[1] = 1; g[2] = 2; g[3] = 3
      37          g[4 * 9 + 0] = 4; g[5 * 9 + 0] = 5

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/TestViewModel.kt)
  ⎿  Wrote 85 lines to ../../../user/soduku-trainer/app/src/main/kotlin/com
     /dividetask/sudokutrainer/ui/test/TestViewModel.kt
       1 package com.dividetask.sudokutrainer.ui.test
       2 
       3 import androidx.lifecycle.ViewModel
       4 import androidx.lifecycle.ViewModelProvider
       5 import androidx.lifecycle.viewModelScope
       6 import androidx.lifecycle.viewmodel.CreationExtras
       7 import com.dividetask.sudokutrainer.data.GameConfig
       8 import com.dividetask.sudokutrainer.domain.GameState
       9 import com.dividetask.sudokutrainer.domain.HintEngine
      10 import com.dividetask.sudokutrainer.ui.game.HintAnimator
     … +82 lines (ctrl+o to expand)

● Now the screen:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/TestScreen.kt)
  ⎿  Wrote 88 lines to ../../../user/soduku-trainer/app/src/main/kotlin/com
     /dividetask/sudokutrainer/ui/test/TestScreen.kt
       1 package com.dividetask.sudokutrainer.ui.test
       2 
       3 import androidx.compose.foundation.layout.Arrangement
       4 import androidx.compose.foundation.layout.Box
       5 import androidx.compose.foundation.layout.Column
       6 import androidx.compose.foundation.layout.Row
       7 import androidx.compose.foundation.layout.fillMaxSize
       8 import androidx.compose.foundation.layout.fillMaxWidth
       9 import androidx.compose.foundation.layout.padding
      10 import androidx.compose.material3.Button
     … +81 lines (ctrl+o to expand)

● Now wire navigation and the menu button:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/Navigation.kt)
  ⎿  Added 18 lines
      13  import com.dividetask.sudokutrainer.ui.game.GameScreen
      14  import com.dividetask.sudokutrainer.ui.game.GameViewModel
      15  import com.dividetask.sudokutrainer.ui.menu.MainMenuScreen
      16 +import com.dividetask.sudokutrainer.ui.test.TestScreen         
      17 +import com.dividetask.sudokutrainer.ui.test.TestViewModel      
      18  
      19  private object Routes {
      20      const val MENU = "menu"
      21      const val GAME = "game/{difficulty}"
      22 +    const val TEST = "test"                                    
      23      fun game(difficulty: Difficulty) = "game/${difficulty.name}
          "
      24  }
      25  
     ...
      32      ) {
      33          composable(Routes.MENU) {
      34              MainMenuScreen(
      35 +                showTestButton = config.showTestButton,        
      36                  onStartGame = { difficulty ->
      37                      navController.navigate(Routes.game(difficul
          ty))
      38                  },
      39 +                onStartTest = {                                
      40 +                    navController.navigate(Routes.TEST)        
      41 +                },                                             
      42              )
      43          }
      44          composable(
     ...
      59                  },
      60              )
      61          }
      62 +        composable(Routes.TEST) {                              
      63 +            val viewModel: TestViewModel = viewModel(          
      64 +                factory = TestViewModel.factory(config),       
      65 +            )                                                  
      66 +            TestScreen(                                        
      67 +                viewModel = viewModel,                         
      68 +                onExit = {                                     
      69 +                    navController.popBackStack(Routes.MENU, inc
         +lusive = false)                                                
      70 +                },                                             
      71 +            )                                                  
      72 +        }                                                      
      73      }
      74  }

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/menu/MainMenuScreen.kt)
  ⎿  Added 18 lines
       8  import androidx.compose.foundation.rememberScrollState
       9  import androidx.compose.foundation.verticalScroll
      10  import androidx.compose.material3.Button
      11 +import androidx.compose.material3.ButtonDefaults               
      12  import androidx.compose.material3.MaterialTheme
      13  import androidx.compose.material3.Scaffold
      14  import androidx.compose.material3.Text
      15  import androidx.compose.runtime.Composable
      16  import androidx.compose.ui.Alignment
      17  import androidx.compose.ui.Modifier
      18 +import androidx.compose.ui.graphics.Color                      
      19  import androidx.compose.ui.text.style.TextAlign
      20  import androidx.compose.ui.unit.dp
      21  import com.dividetask.sudokutrainer.domain.Difficulty
      22  
      23  @Composable
      24  fun MainMenuScreen(
      25 +    showTestButton: Boolean,                                   
      26      onStartGame: (Difficulty) -> Unit,
      27 +    onStartTest: () -> Unit,                                   
      28  ) {
      29      Scaffold { inner ->
      30          Column(
     ...
      53                      )
      54                  }
      55              }
      56 +            if (showTestButton) {                              
      57 +                Button(                                        
      58 +                    onClick = onStartTest,                     
      59 +                    modifier = Modifier.fillMaxWidth(),        
      60 +                    colors = ButtonDefaults.buttonColors(      
      61 +                        containerColor = Color(0xFF6A1B9A),    
      62 +                    ),                                         
      63 +                ) {                                            
      64 +                    Text(                                      
      65 +                        text = "Test",                         
      66 +                        style = MaterialTheme.typography.titleM
         +edium,                                                         
      67 +                    )                                          
      68 +                }                                              
      69 +            }                                                  
      70          }
      71      }
      72  }

● Let me verify the file states and rerun tests.

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:classes UP-TO-DATE
     > Task :domain:jar UP-TO-DATE
     > Task :domain:compileTestKotlin UP-TO-DATE
     … +7 lines (ctrl+o to expand)

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/TestViewModel.kt)
  ⎿  Removed 2 lines
      70          _exitSignal.value = true
      71      }
      72  
      73 -    val gameConfig: GameConfig get() = config                  
      74 -                                                               
      73      companion object {
      74          private const val INTRO_DELAY_MS = 800L
      75          private const val SETTLE_DELAY_MS = 1500L

● Bash(git status && echo "---" && git diff --stat HEAD)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Your branch is up to date with 'origin/claude/add-sudoku-hint-button-L
     1spT'.
     … +24 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Make hints interruptible; add Test menu for previewing animations…)
  ⎿  [claude/add-sudoku-hint-button-L1spT ea3a655] Make hints interruptible
     ; add Test menu for previewing animations
      10 files changed, 400 insertions(+), 60 deletions(-)
     … +15 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT && git log --oneline
      origin/claude/add-sudoku-hint-button-L1spT -3 && echo "---" && git rebase
      origin/claud…)
  ⎿  From http://127.0.0.1:42527/git/dividetask/soduku-trainer
      * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        9997bf5..f7d45f3  claude/add-sudoku-hint-button-L1spT -> origin/cla
     … +10 lines (ctrl+o to expand)

● Pushed d28b019.

  Part 1 — Skip mid-animation:
  - HintAnimator (app/.../ui/game/HintAnimator.kt) owns the animation coroutine.
  skip() cancels the job and calls completeXSingleHint immediately, so the
  placement still goes through history (so the existing undo works).
  - GameViewModel.onHint routes a second press through animator.skip() when one
  is in flight.
  - Controls.kt keeps the button enabled during the Hinting phase and relabels
  it Skip.

  Part 2 — Test menu:
  - GameConfig.showTestButton (YAML key show_test_button, default true) gates a
  new purple Test button under Extreme on the main menu.
  - HintTestCases (app/.../ui/test/HintTestCases.kt) holds the preview puzzles.
  Order is newest first: Hidden Single, Naked Single. Each case keeps
  hand-picked givens and uses SudokuSolver.solveOne to derive a consistent
  Puzzle.solution. Adding the next technique = prepend one build(...) call.
  - TestViewModel runs an init coroutine that, for each case: resets state to
  the puzzle, waits 800 ms, calls HintEngine.find, runs
  HintAnimator.launch(...).join(), waits 1.5 s, advances. After the last case it
  sets exitSignal and the screen pops back to the menu.
  - TestScreen shows a header ("1 of 2 — Hidden Single"), the Board, and Skip /
  Exit buttons. Tap callbacks on the Board are wired to no-ops since the test is
  non-interactive.

  Heads up — there's a tradeoff in the Test screen I didn't optimize: pressing
  Skip during the 800 ms intro delay before the animation starts is a no-op.
  Easy to add later (track sequencer state + cancel) if you find it annoying.
  The two test puzzles are very sparse (4 and 8 clues), so solveOne does fill in
  the rest with a backtracker — should be milliseconds, but if you ever notice
  startup latency, that's the place.
❯ Test causes it to crash. You can prebuild the soduku puzzles for test, we     
  don't need a new one each time                                                

● The crash is almost certainly SudokuSolver.solveOne thrashing on the
  very-sparse grids. Let me verify and precompute the solutions.

● Write(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokutr
       ainer/domain/PrecomputeTestSolutions.kt)
  ⎿  Wrote 39 lines to ../../../user/soduku-trainer/domain/src/test/kotlin/
     com/dividetask/sudokutrainer/domain/PrecomputeTestSolutions.kt
       2 package com.dividetask.sudokutrainer.domain
       3 import org.junit.jupiter.api.Test
       4 
       5 /**
       6  * One-shot helper: prints SudokuSolver.solveOne results for the
       7  * grids so we can paste them as constants into the app module. 
         Not a
       8  * real test — assertions are absent on purpose.
       9  */
      10 class PrecomputeTestSolutions {
     … +33 lines (ctrl+o to expand)

● Bash(./gradlew :domain:test --tests "*PrecomputeTestSolutions*"
      -PincludeApp=false --no-daemon -i 2>&1 | grep -E "^(\-\-\-|[0-9, ]+,$|NO
      SOLUTION|BUILD)" | head -40)
  ⎿  Running in the background (↓ to manage)

● Bash(wait $(jobs -p) 2>/dev/null; cat /tmp/claude-0/-home-user-soduku-trainer/
      fa5a9dd8-d10e-4a97-b622-9ff43bae9be2/tasks/bsgwldk46.output 2>/dev/null |
      tail -60)
  ⎿  (No output)

  Read 1 file, listed 1 directory (ctrl+o to expand)

● Bash(./gradlew :domain:test --tests "*PrecomputeTestSolutions*"
      -PincludeApp=false --no-daemon -i 2>&1 | tail -80)
  ⎿  Running in the background (↓ to manage)

● Monitor(Wait for gradle test completion)
  ⎿  Monitor started · task b9324dyki · timeout 300s

  Searched for 1 pattern, read 1 file, listed 1 directory (ctrl+o to expand)

● Waiting for the build to complete...

● Bash(sleep 30; wc -l /tmp/claude-0/-home-user-soduku-trainer/fa5a9dd8-d10e-4a9
      7-b622-9ff43bae9be2/tasks/bx1ahnao7.output 2>/dev/null; echo "---"; ps -ef
      | grep -i gr…)
  ⎿  Error: Blocked: sleep 30 followed by: wc -l /tmp/claude-0/-home-user-soduku
     -trainer/fa5a9dd8-d10e-4a97-b622-9ff43bae9be2/tasks/bx1ahnao7.output echo 
     "---" ps -ef grep -i gradle head -5. To wait for a condition, use Monitor 
     with an until-loop (e.g. `until <check>; do sleep 2; done`). To wait for a 
     command you started, use run_in_background: true. Do not chain shorter 
     sleeps to work around this block.

● Bash(ps -ef | grep -i gradle | head -5; echo "---"; wc -c
      /tmp/claude-0/-home-user-soduku-trainer/fa5a9dd8-d10e-4a97-b622-9ff43bae9b
      e2/tasks/bx1ahnao7.output)
  ⎿  root      1006   533  0 04:52 ?        00:00:00 /bin/bash -c source /r
     oot/.claude/shell-snapshots/snapshot-bash-1779252756230-7n4xzh.sh 2>/d
     ev/null || true && export BUN_OPTIONS="--smol${BUN_OPTIONS:+ $BUN_OPTI
     … +76 lines (ctrl+o to expand)

● The hung process confirms solveOne is exponential on these sparse grids. Let
  me kill it and switch to hardcoded complete grids.

● Bash(pkill -f "GradleWorkerMain" 2>&1; pkill -f gradle 2>&1; sleep 2; ps -ef |
      grep -E "gradle|kotlin" | grep -v grep | head -5)
  ⎿  Error: Exit code 144

● Bash(ps -ef | grep -E "gradle|kotlin|java" | grep -v grep | head -10)
  ⎿  root      1506     1  2 04:53 ?        00:00:17 [java] <defunct>

● Background command "Re-run precompute foreground" completed (exit code 0)

● Background command "Precompute solutions" completed (exit code 0)

● Monitor event: "Wait for gradle test completion"

● Monitor "Wait for gradle test completion" stream ended

● Switching HintTestCases to hardcoded givens + a hardcoded solution. No more
  solveOne.

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/HintTestCases.kt)
  ⎿  Added 43 lines, removed 29 lines
       2  
       3  import com.dividetask.sudokutrainer.domain.Difficulty
       4  import com.dividetask.sudokutrainer.domain.Puzzle
       5 -import com.dividetask.sudokutrainer.domain.SudokuSolver        
       5  
       6  /**
       7   * Pre-built puzzles whose next hint is guaranteed to be a spec
          ific
       8   * technique. Used by the "Test" menu to preview each teaching 
          animation
       9   * in turn. Ordering is newest-technique-first.
      10 + *                                                             
      11 + * Each case shares the same complete grid as its `solution`; t
         +he                                                             
      12 + * `givens` are a small subset chosen so [com.dividetask.sudoku
         +trainer                                                        
      13 + * .domain.HintEngine.find] reports the intended technique on t
         +he next                                                        
      14 + * deduction. Solutions are hardcoded — no on-device solver cal
         +l —                                                            
      15 + * because backtracking on extremely sparse grids is exponentia
         +l.                                                             
      16   */
      17  object HintTestCases {
      18  
      19      data class Case(val title: String, val puzzle: Puzzle)
      20  
      16 -    val cases: List<Case> by lazy {                            
      17 -        listOf(                                                
      18 -            build("Hidden Single", "test-hidden-single", hidden
         -SingleGivens()),                                               
      19 -            build("Naked Single", "test-naked-single", nakedSin
         -gleGivens()),                                                  
      20 -        )                                                      
      21 -    }                                                          
      21 +    val cases: List<Case> = listOf(                            
      22 +        build(                                                 
      23 +            title = "Hidden Single",                           
      24 +            id = "test-hidden-single",                         
      25 +            // The four 5s pin every cell of box 0 except (0,0)
         + — col 1 via                                                   
      26 +            // (3,1), col 2 via (8,2), row 1 via (1,5), row 2 v
         +ia (2,6).                                                      
      27 +            givenIndices = intArrayOf(                         
      28 +                3 * 9 + 1,                                     
      29 +                1 * 9 + 5,                                     
      30 +                2 * 9 + 6,                                     
      31 +                8 * 9 + 2,                                     
      32 +            ),                                                 
      33 +        ),                                                     
      34 +        build(                                                 
      35 +            title = "Naked Single",                            
      36 +            id = "test-naked-single",                          
      37 +            // Row 0 minus (0,0). The eight clues cover every d
         +igit but                                                       
      38 +            // five, so (0,0) has only `5` as its candidate.   
      39 +            givenIndices = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8), 
      40 +        ),                                                     
      41 +    )                                                          
      42  
      23 -    /** (0,0) is a hidden single for 5 in box 0. */            
      24 -    private fun hiddenSingleGivens(): IntArray {               
      25 -        val g = IntArray(81)                                   
      26 -        g[3 * 9 + 1] = 5                                       
      27 -        g[4 * 9 + 2] = 5                                       
      28 -        g[1 * 9 + 5] = 5                                       
      29 -        g[2 * 9 + 7] = 5                                       
      30 -        return g                                               
      31 -    }                                                          
      43 +    /** A complete, valid 9x9 grid; every case uses this as its
         + solution. */                                                  
      44 +    private val CANONICAL_SOLUTION: IntArray = intArrayOf(     
      45 +        5, 3, 4, 6, 7, 8, 9, 1, 2,                             
      46 +        6, 7, 2, 1, 9, 5, 3, 4, 8,                             
      47 +        1, 9, 8, 3, 4, 2, 5, 6, 7,                             
      48 +        8, 5, 9, 7, 6, 1, 4, 2, 3,                             
      49 +        4, 2, 6, 8, 5, 3, 7, 9, 1,                             
      50 +        7, 1, 3, 9, 2, 4, 8, 5, 6,                             
      51 +        9, 6, 1, 5, 3, 7, 2, 8, 4,                             
      52 +        2, 8, 7, 4, 1, 9, 6, 3, 5,                             
      53 +        3, 4, 5, 2, 8, 6, 1, 7, 9,                             
      54 +    )                                                          
      55  
      33 -    /** (0,0) is a naked single (only 9 fits). */              
      34 -    private fun nakedSingleGivens(): IntArray {                
      35 -        val g = IntArray(81)                                   
      36 -        g[1] = 1; g[2] = 2; g[3] = 3                           
      37 -        g[4 * 9 + 0] = 4; g[5 * 9 + 0] = 5                     
      38 -        g[1 * 9 + 1] = 6; g[1 * 9 + 2] = 7; g[2 * 9 + 1] = 8   
      39 -        return g                                               
      40 -    }                                                          
      41 -                                                               
      42 -    private fun build(title: String, id: String, givens: IntArr
         -ay): Case {                                                    
      43 -        val solution = SudokuSolver.solveOne(givens)           
      44 -            ?: error("Test puzzle $id has no solution")        
      56 +    private fun build(title: String, id: String, givenIndices: 
         +IntArray): Case {                                              
      57 +        val givens = IntArray(81)                              
      58 +        for (idx in givenIndices) givens[idx] = CANONICAL_SOLUT
         +ION[idx]                                                       
      59          return Case(
      60              title = title,
      61              puzzle = Puzzle(
      62                  id = id,
      63                  difficulty = Difficulty.Beginner,
      64                  givens = givens.toList(),
      51 -                solution = solution.toList(),                  
      65 +                solution = CANONICAL_SOLUTION.toList(),        
      66              ),
      67          )
      68      }

● Bash(rm /home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudoku
      trainer/domain/PrecomputeTestSolutions.kt)  ⎿  Done

● Add a domain test that mirrors the test-case construction, so we catch
  regressions in the deduction without depending on the app module:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Removed 14 lines
       91  
       92      @Test
       93      fun `returns null on a fully solved grid`() {
       94 -        val solved = intArrayOf(                              
       95 -            5, 3, 4, 6, 7, 8, 9, 1, 2,                        
       96 -            6, 7, 2, 1, 9, 5, 3, 4, 8,                        
       97 -            1, 9, 8, 3, 4, 2, 5, 6, 7,                        
       98 -            8, 5, 9, 7, 6, 1, 4, 2, 3,                        
       99 -            4, 2, 6, 8, 5, 3, 7, 9, 1,                        
      100 -            7, 1, 3, 9, 2, 4, 8, 5, 6,                        
      101 -            9, 6, 1, 5, 3, 7, 2, 8, 4,                        
      102 -            2, 8, 7, 4, 1, 9, 6, 3, 5,                        
      103 -            3, 4, 5, 2, 8, 6, 1, 7, 9,                        
      104 -        )                                                     
      105 -        assertNull(HintEngine.find(solved))                   
      106 -    }                                                         
      107 -}                                                             

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 39 lines
       90      }
       91  
       92      @Test
       93 +    fun `Test-menu Hidden Single preview grid yields a HiddenS
          +ingle`() {                                                    
       94 +        val solution = CANONICAL_SOLUTION                     
       95 +        val givenIndices = intArrayOf(3 * 9 + 1, 1 * 9 + 5, 2 
          +* 9 + 6, 8 * 9 + 2)                                           
       96 +        val grid = IntArray(81)                               
       97 +        for (idx in givenIndices) grid[idx] = solution[idx]   
       98 +        val hint = HintEngine.find(grid)                      
       99 +        assertTrue(hint is HintEngine.HiddenSingle, "expected 
          +HiddenSingle, got $hint")                                     
      100 +        hint as HintEngine.HiddenSingle                       
      101 +        assertEquals(0, hint.targetCell)                      
      102 +        assertEquals(5, hint.solutionDigit)                   
      103 +        assertEquals(HintEngine.UnitKind.Box, hint.unit.kind) 
      104 +    }                                                         
      105 +                                                              
      106 +    @Test                                                     
      107 +    fun `Test-menu Naked Single preview grid yields a NakedSin
          +gle`() {                                                      
      108 +        val solution = CANONICAL_SOLUTION                     
      109 +        val givenIndices = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8) 
      110 +        val grid = IntArray(81)                               
      111 +        for (idx in givenIndices) grid[idx] = solution[idx]   
      112 +        val hint = HintEngine.find(grid)                      
      113 +        assertTrue(hint is HintEngine.NakedSingle, "expected N
          +akedSingle, got $hint")                                       
      114 +        hint as HintEngine.NakedSingle                        
      115 +        assertEquals(0, hint.targetCell)                      
      116 +        assertEquals(5, hint.solutionDigit)                   
      117 +    }                                                         
      118 +                                                              
      119 +    private val CANONICAL_SOLUTION = intArrayOf(              
      120 +        5, 3, 4, 6, 7, 8, 9, 1, 2,                            
      121 +        6, 7, 2, 1, 9, 5, 3, 4, 8,                            
      122 +        1, 9, 8, 3, 4, 2, 5, 6, 7,                            
      123 +        8, 5, 9, 7, 6, 1, 4, 2, 3,                            
      124 +        4, 2, 6, 8, 5, 3, 7, 9, 1,                            
      125 +        7, 1, 3, 9, 2, 4, 8, 5, 6,                            
      126 +        9, 6, 1, 5, 3, 7, 2, 8, 4,                            
      127 +        2, 8, 7, 4, 1, 9, 6, 3, 5,                            
      128 +        3, 4, 5, 2, 8, 6, 1, 7, 9,                            
      129 +    )                                                         
      130 +                                                              
      131 +    @Test                                                     
      132      fun `returns null on a fully solved grid`() {

● Now restore the test body I accidentally deleted:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 3 lines
      130  
      131      @Test
      132      fun `returns null on a fully solved grid`() {
      133 +        assertNull(HintEngine.find(CANONICAL_SOLUTION))       
      134 +    }                                                         
      135 +}                                                             

  Read 1 file (ctrl+o to expand)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon --rerun-tasks 2>&1
      | tail -20)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +16 lines (ctrl+o to expand)

● All tests pass (including the two new ones that exercise the test-menu grids
  through HintEngine.find). Commit and push:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Hardcode Test-menu puzzles to avoid solveOne hang…)
  ⎿  [claude/add-sudoku-hint-button-L1spT bc69336] Hardcode Test-menu puzzl
     es to avoid solveOne hang
      2 files changed, 83 insertions(+), 41 deletions(-)
     … +8 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT && git rebase
      origin/claude/add-sudoku-hint-button-L1spT 2>&1 | tail -5 && git push 2>&1
      | tail -5)
  ⎿  From http://127.0.0.1:46007/git/dividetask/soduku-trainer
      * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        d28b019..4c5190d  claude/add-sudoku-hint-button-L1spT -> origin/cla
     … +6 lines (ctrl+o to expand)

● Pushed 6d80a63.

  Root cause: SudokuSolver.solveOne is a vanilla backtracker; on the 4-clue
  Hidden Single grid every empty cell has 8–9 candidates and the search tree is
  effectively unbounded. The call ran on the main thread when TestViewModel was
  instantiated, hanging the UI → ANR/crash.

  Fix: HintTestCases now holds a single hardcoded CANONICAL_SOLUTION (a valid
  completed grid). Each case is built with a tiny givenIndices list (4 cells for
  Hidden Single, 8 for Naked Single), and the puzzle's solution is just
  CANONICAL_SOLUTION.toList() — no solver call at construction. Added two domain
  tests that run HintEngine.find on the same grids to assert the next deduction
  is the intended technique.

❯ The app keeps crashing when I hit test. It used to freeze without giving an   
  error but not it completely crashes                                           

● Let me check for anything that might throw at construction:

  Read 3 files (ctrl+o to expand)

● Found it. In a Kotlin object, properties initialize top-to-bottom. cases is
  declared above CANONICAL_SOLUTION, so when build() runs during cases
  initialization, CANONICAL_SOLUTION is still null — NPE on the very first
  access. (My domain test never hit this because the test inlines the solution.)

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/HintTestCases.kt)
  ⎿  Added 19 lines, removed 13 lines
      18  
      19      data class Case(val title: String, val puzzle: Puzzle)
      20  
      21 +    /**                                                        
      22 +     * A complete, valid 9x9 grid; every case uses this as its 
         +solution.                                                      
      23 +     *                                                         
      24 +     * Declared *before* [cases] because Kotlin object properti
         +es                                                             
      25 +     * initialize top-to-bottom: [cases]'s initializer calls [b
         +uild],                                                         
      26 +     * which reads this constant, so it must already hold a val
         +ue.                                                            
      27 +     */                                                        
      28 +    private val CANONICAL_SOLUTION: IntArray = intArrayOf(     
      29 +        5, 3, 4, 6, 7, 8, 9, 1, 2,                             
      30 +        6, 7, 2, 1, 9, 5, 3, 4, 8,                             
      31 +        1, 9, 8, 3, 4, 2, 5, 6, 7,                             
      32 +        8, 5, 9, 7, 6, 1, 4, 2, 3,                             
      33 +        4, 2, 6, 8, 5, 3, 7, 9, 1,                             
      34 +        7, 1, 3, 9, 2, 4, 8, 5, 6,                             
      35 +        9, 6, 1, 5, 3, 7, 2, 8, 4,                             
      36 +        2, 8, 7, 4, 1, 9, 6, 3, 5,                             
      37 +        3, 4, 5, 2, 8, 6, 1, 7, 9,                             
      38 +    )                                                          
      39 +                                                               
      40      val cases: List<Case> = listOf(
      41          build(
      42              title = "Hidden Single",
     ...
      59          ),
      60      )
      61  
      43 -    /** A complete, valid 9x9 grid; every case uses this as its
         - solution. */                                                  
      44 -    private val CANONICAL_SOLUTION: IntArray = intArrayOf(     
      45 -        5, 3, 4, 6, 7, 8, 9, 1, 2,                             
      46 -        6, 7, 2, 1, 9, 5, 3, 4, 8,                             
      47 -        1, 9, 8, 3, 4, 2, 5, 6, 7,                             
      48 -        8, 5, 9, 7, 6, 1, 4, 2, 3,                             
      49 -        4, 2, 6, 8, 5, 3, 7, 9, 1,                             
      50 -        7, 1, 3, 9, 2, 4, 8, 5, 6,                             
      51 -        9, 6, 1, 5, 3, 7, 2, 8, 4,                             
      52 -        2, 8, 7, 4, 1, 9, 6, 3, 5,                             
      53 -        3, 4, 5, 2, 8, 6, 1, 7, 9,                             
      54 -    )                                                          
      55 -                                                               
      62      private fun build(title: String, id: String, givenIndices: 
          IntArray): Case {
      63          val givens = IntArray(81)
      64          for (idx in givenIndices) givens[idx] = CANONICAL_SOLUT
          ION[idx]

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Fix Test-menu NPE from forward reference in HintTestCases…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 435aeb2] Fix Test-menu NPE from f
     orward reference in HintTestCases
      1 file changed, 19 insertions(+), 13 deletions(-)
     … +9 lines (ctrl+o to expand)

● Pushed 08f79d8.

  The bug: In a Kotlin object, property initializers run top-to-bottom. cases = 
  listOf(build(...)) ran first and called build(), which read CANONICAL_SOLUTION
  — but CANONICAL_SOLUTION was still its JVM default (null for an IntArray
  reference). First subscript access on it threw NullPointerException when
  TestViewModel was constructed, crashing the screen the moment Test was tapped.

  Fix: Move the CANONICAL_SOLUTION declaration above cases. The domain tests
  didn't catch this because each test inlines the solution as a local variable;
  the bug only existed in the forward-reference inside the app-module object.

❯ Looks good, and runs without crashing. What hint do we need to solve level 3? 

● Level 3 is Naked Pair: when two cells in the same unit (row, column, or box)
  both have exactly the same two pencil candidates {a, b}, then a and b are
  forced into those two cells in some order — so every other cell in the unit
  can eliminate both a and b from its notes.

  The big teaching shift from levels 1–2 is that a Naked Pair doesn't place a
  digit — it only eliminates candidates. So the animation should highlight the
  pair (green) and the unit (frame), sweep the unit's other cells erasing a and
  b notes from each, then end without filling anything in. That likely means a
  new completion path in the reducer (no Move.SetValue; a
  Move.PencilEliminations that records every erased mark for undo), and possibly
  a small UI tweak so the player notices that the hint prepared the board
  rather than finishing a cell.

  One catch worth flagging up front: a naked pair often won't apply on a board
  with no player notes, because the deduction reads candidates, not placed
  values. If the player hasn't penciled anything yet, the hint engine would need
  to either auto-compute candidates first or refuse the hint with a "fill in
  some notes first" message — pick one before implementing.

❯ Ok. Let's add another strategy before naked pair. I am not sure what to call  
  it though. Instead of doing one cell it will go through every cell removing   
  any note that can be eliminated from a number in the square, a number in the  
  row, or a number in the column. If there are no player notes then we will add 
  all player notes before applying this strategy. Since this strategy runs      
  before the naked pair then there should always be player notes for the naked  
  pair.                                                                         
  Please come up with a name for this new strategy that goes before the naked   
  pair                                                                          

● I'd call it "Candidate Sweep." It captures both halves of the move — sweep
  through every empty cell, and what you're touching is the candidate set. When
  no player notes exist, the technique starts by penciling all candidates in (an
  implicit "fill") and then the sweep prunes; when notes already exist, it just
  prunes.

  A few alternatives if "Candidate Sweep" doesn't land:

  - Pencil Pass — friendlier, alliterative, but a little vague about what it
  does.
  - Note Cleanup — very plain English; reads as a chore rather than a deduction
  step.
  - Cross-Hatching — the traditional Sudoku name for the visual row/col/box scan
  that produces the same eliminations, but in print it usually means the human
  technique for finding hidden singles, so it would be slightly misleading here.
  - Basic Elimination — technically accurate but generic; doesn't differentiate
  from naked-pair style elimination later on.

  My pick is Candidate Sweep because it leaves room for future "sweep" variants
  (e.g., a unit-only sweep) and reads naturally as the prerequisite that primes
  the board for pair/triple techniques.

  Which name do you want to use?
❯ Candidate Sweep works.                                                        

  Searched for 1 pattern (ctrl+o to expand)

● Let me update the engine first.

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 41 lines, removed 10 lines
      14      /** A technique that can be applied to advance the solve. *
          /
      15      sealed interface Hint {
      16          val techniqueName: String
      17 -        val targetCell: Int                                    
      18 -        val solutionDigit: Int                                 
      17      }
      18  
      19      /**
     ...
      22       * values prove that digit can't go in the target.
      23       */
      24      data class NakedSingle(
      27 -        override val targetCell: Int,                          
      28 -        override val solutionDigit: Int,                       
      25 +        val targetCell: Int,                                   
      26 +        val solutionDigit: Int,                                
      27          val eliminators: Map<Int, Set<Int>>,
      28      ) : Hint {
      29          override val techniqueName: String = "Naked Single"
     ...
      42       * have no entry.
      43       */
      44      data class HiddenSingle(
      47 -        override val targetCell: Int,                          
      48 -        override val solutionDigit: Int,                       
      45 +        val targetCell: Int,                                   
      46 +        val solutionDigit: Int,                                
      47          val unit: HintUnit,
      48          val eliminators: Map<Int, Set<Int>>,
      49      ) : Hint {
     ...
      51      }
      52  
      53      /**
      56 -     * Search [grid] for the easiest technique that produces a 
         -placement.                                                     
      57 -     * Returns null if no implemented technique applies.       
      54 +     * Prune impossible pencil marks from every empty cell — an
         +y candidate                                                    
      55 +     * digit blocked by a peer in the same row, column, or box 
         +is removed.                                                    
      56 +     * When the board has no player notes at all, the sweep als
         +o *adds* the                                                   
      57 +     * full 1..9 candidate set to every empty cell before pruni
         +ng, so                                                         
      58 +     * downstream techniques (pairs, etc.) have notes to work w
         +ith.                                                           
      59       *
      59 -     * [grid] is a flat 81-entry array with 0 = empty.         
      60 +     * [plan] enumerates the cells that actually lose at least 
         +one mark, in                                                   
      61 +     * row-major order. [initialFill] is true iff the sweep nee
         +ds to seed                                                     
      62 +     * candidates because the board had no notes at the start. 
      63       */
      61 -    fun find(grid: IntArray): Hint? {                          
      64 +    data class CandidateSweep(                                 
      65 +        val initialFill: Boolean,                              
      66 +        val plan: List<CellPlan>,                              
      67 +    ) : Hint {                                                 
      68 +        override val techniqueName: String = "Candidate Sweep" 
      69 +                                                               
      70 +        /**                                                    
      71 +         * [doomedDigits] are the digits that will be erased fr
         +om [cellIdx];                                                  
      72 +         * [eliminatorPeers] are the peer cells outside this ce
         +ll whose                                                       
      73 +         * values prove the eliminations.                      
      74 +         */                                                    
      75 +        data class CellPlan(                                   
      76 +            val cellIdx: Int,                                  
      77 +            val doomedDigits: Set<Int>,                        
      78 +            val eliminatorPeers: Set<Int>,                     
      79 +        )                                                      
      80 +    }                                                          
      81 +                                                               
      82 +    /**                                                        
      83 +     * Search [grid] for the easiest technique that produces a 
         +placement                                                      
      84 +     * or note pruning. Returns null if no implemented techniqu
         +e applies.                                                     
      85 +     *                                                         
      86 +     * [grid] is a flat 81-entry array with 0 = empty. [marks] 
         +is an                                                          
      87 +     * optional per-cell snapshot of pencil-mark *digits* (colo
         +rs aren't                                                      
      88 +     * needed by the engine); when omitted, the engine assumes 
         +no marks.                                                      
      89 +     */                                                        
      90 +    fun find(grid: IntArray, marks: Array<Set<Int>>? = null): H
         +int? {                                                         
      91          require(grid.size == 81)
      92 +        require(marks == null || marks.size == 81)             
      93          findNakedSingle(grid)?.let { return it }
      94          findHiddenSingle(grid)?.let { return it }
      95 +        findCandidateSweep(grid, marks)?.let { return it }     
      96          return null
      97      }
      98  

● Now add findCandidateSweep to the engine, after findHiddenSingle:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 37 lines
      162          return null
      163      }
      164  
      165 +    private fun findCandidateSweep(grid: IntArray, marks: Arra
          +y<Set<Int>>?): CandidateSweep? {                              
      166 +        val anyMarks = marks != null && marks.any { it.isNotEm
          +pty() }                                                       
      167 +        val initialFill = !anyMarks                           
      168 +                                                              
      169 +        val plan = mutableListOf<CandidateSweep.CellPlan>()   
      170 +        for (idx in 0 until 81) {                             
      171 +            if (grid[idx] != 0) continue                      
      172 +            val candidates = if (initialFill) ALL_DIGITS else 
          +marks!![idx]                                                  
      173 +            if (candidates.isEmpty()) continue                
      174 +            val row = idx / 9                                 
      175 +            val col = idx % 9                                 
      176 +            val doomed = mutableSetOf<Int>()                  
      177 +            val peers = mutableSetOf<Int>()                   
      178 +            for (d in candidates) {                           
      179 +                val blockers = peersHolding(grid, row, col, d)
      180 +                if (blockers.isNotEmpty()) {                  
      181 +                    doomed += d                               
      182 +                    peers += blockers                         
      183 +                }                                             
      184 +            }                                                 
      185 +            if (doomed.isNotEmpty()) {                        
      186 +                plan += CandidateSweep.CellPlan(              
      187 +                    cellIdx = idx,                            
      188 +                    doomedDigits = doomed,                    
      189 +                    eliminatorPeers = peers,                  
      190 +                )                                             
      191 +            }                                                 
      192 +        }                                                     
      193 +                                                              
      194 +        // When the board already has notes and nothing to pru
          +ne, there's                                                   
      195 +        // no sweep work — let later techniques try instead.  
      196 +        if (!initialFill && plan.isEmpty()) return null       
      197 +        return CandidateSweep(initialFill = initialFill, plan 
          += plan)                                                       
      198 +    }                                                         
      199 +                                                              
      200 +    private val ALL_DIGITS: Set<Int> = (1..9).toSet()         
      201 +                                                              
      202      /** Peer cells of [cellIdx] that lie outside [unitCells] a
           nd hold [digit]. */
      203      private fun peersOutsideUnitHolding(
      204          grid: IntArray,
  ⎿  You've hit your session limit · resets 8:10am (UTC)
     /upgrade or /usage-credits to finish what you’re working on.

❯ Resume                                                                        

● Continuing with HintUi, Move, and the reducer.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Added 18 lines
      53          val targetOriginalNotes: Map<Int, GuessColor> = emptyMa
          p(),
      54          val clearedPeerMarks: List<AutoClearedMark> = emptyList
          (),
      55      ) : HintUi
      56 +                                                               
      57 +    /**                                                        
      58 +     * Candidate-sweep animation in progress.                  
      59 +     *                                                         
      60 +     * - [initialFill] indicates the board needed candidates fi
         +lled in.                                                       
      61 +     * - [currentCell] is the cell currently being pruned (or n
         +ull for                                                        
      62 +     *   the settle frame).                                    
      63 +     * - [eliminatorCells] are the peers whose values prove the
         + current                                                       
      64 +     *   cell's eliminations.                                  
      65 +     * - [priorMarks] is the full pencil-mark snapshot captured
         + at begin;                                                     
      66 +     *   completion uses it to build the consolidated undo move
         +.                                                              
      67 +     */                                                        
      68 +    data class CandidateSweep(                                 
      69 +        val initialFill: Boolean,                              
      70 +        val currentCell: Int? = null,                          
      71 +        val eliminatorCells: Set<Int> = emptySet(),            
      72 +        val priorMarks: List<AutoClearedMark> = emptyList(),   
      73 +    ) : HintUi                                                 
      74  }

● Now Move — add PencilSweep and remove the abstract row/col so the new move
  doesn't need fake values:
  ⎿  Error writing file

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/Move.kt)
  ⎿  Added 17 lines, removed 9 lines
      1  package com.dividetask.sudokutrainer.domain
      2  
      3 -/** A pencil mark that was auto-cleared from a peer cell during 
        -a Blue/Solve write. */                                          
      3 +/** A pencil mark added or cleared on a specific cell — used by 
        +undo. */                                                        
      4  data class AutoClearedMark(
      5      val row: Int,
      6      val col: Int,
     ...
       9  )
      10  
      11  sealed interface Move {
      12 -    val row: Int                                               
      13 -    val col: Int                                               
      12  
      13      data class SetValue(
      16 -        override val row: Int,                                 
      17 -        override val col: Int,                                 
      14 +        val row: Int,                                          
      15 +        val col: Int,                                          
      16          val newValue: Int,
      17          val newValueColor: GuessColor,
      18          val clearedMarks: Map<Int, GuessColor>,
     ...
      22      }
      23  
      24      data class ClearValue(
      27 -        override val row: Int,                                 
      28 -        override val col: Int,                                 
      25 +        val row: Int,                                          
      26 +        val col: Int,                                          
      27          val prevValue: Int,
      28          val prevValueColor: GuessColor,
      29      ) : Move {
     ...
      31      }
      32  
      33      data class TogglePencilMark(
      36 -        override val row: Int,                                 
      37 -        override val col: Int,                                 
      34 +        val row: Int,                                          
      35 +        val col: Int,                                          
      36          val digit: Int,
      37          val added: Boolean,
      38          val markColor: GuessColor,
      39      ) : Move {
      40          init { require(digit in 1..9) }
      41      }
      42 +                                                               
      43 +    /**                                                        
      44 +     * One Candidate Sweep step: any number of cells gained or 
         +lost pencil                                                    
      45 +     * marks atomically. [addedMarks] are removed on undo; [rem
         +ovedMarks]                                                     
      46 +     * are restored.                                           
      47 +     */                                                        
      48 +    data class PencilSweep(                                    
      49 +        val addedMarks: List<AutoClearedMark>,                 
      50 +        val removedMarks: List<AutoClearedMark>,               
      51 +    ) : Move                                                   
      52  }

● Refactor GameReducer.undo for the new shape and add the candidate-sweep
  methods:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 35 lines, removed 21 lines
      127          val trimmedHistory = state.history.dropLast(1)
      128  
      129          var s = state
      130 -        // First restore auto-cleared pencil marks from peer c
          -ells.                                                         
      131 -        if (last is Move.SetValue) {                          
      132 -            for (acm in last.autoClearedMarks) {              
      133 -                val peer = s.cellAt(acm.row, acm.col)         
      134 -                s = s.replaceCell(peer.copy(                  
      135 -                    pencilMarks = peer.pencilMarks + (acm.digi
          -t to acm.color),                                              
      130 +        when (last) {                                         
      131 +            is Move.SetValue -> {                             
      132 +                for (acm in last.autoClearedMarks) {          
      133 +                    val peer = s.cellAt(acm.row, acm.col)     
      134 +                    s = s.replaceCell(peer.copy(              
      135 +                        pencilMarks = peer.pencilMarks + (acm.
          +digit to acm.color),                                          
      136 +                    ))                                        
      137 +                }                                             
      138 +                val cell = s.cellAt(last.row, last.col)       
      139 +                s = s.replaceCell(cell.copy(                  
      140 +                    value = null,                             
      141 +                    valueColor = null,                        
      142 +                    pencilMarks = last.clearedMarks,          
      143                  ))
      144              }
      138 -        }                                                     
      139 -                                                              
      140 -        val cell = s.cellAt(last.row, last.col)               
      141 -        val restored: Cell = when (last) {                    
      142 -            is Move.SetValue -> cell.copy(                    
      143 -                value = null,                                 
      144 -                valueColor = null,                            
      145 -                pencilMarks = last.clearedMarks,              
      146 -            )                                                 
      147 -            is Move.ClearValue -> cell.copy(                  
      148 -                value = last.prevValue,                       
      149 -                valueColor = CellColor.Guess(last.prevValueCol
          -or),                                                          
      150 -            )                                                 
      145 +            is Move.ClearValue -> {                           
      146 +                val cell = s.cellAt(last.row, last.col)       
      147 +                s = s.replaceCell(cell.copy(                  
      148 +                    value = last.prevValue,                   
      149 +                    valueColor = CellColor.Guess(last.prevValu
          +eColor),                                                      
      150 +                ))                                            
      151 +            }                                                 
      152              is Move.TogglePencilMark -> {
      153 +                val cell = s.cellAt(last.row, last.col)       
      154                  val marks = if (last.added) {
      155                      cell.pencilMarks - last.digit
      156                  } else {
      157                      cell.pencilMarks + (last.digit to last.mar
           kColor)
      158                  }
      157 -                cell.copy(pencilMarks = marks)                
      159 +                s = s.replaceCell(cell.copy(pencilMarks = mark
          +s))                                                           
      160              }
      161 +            is Move.PencilSweep -> {                          
      162 +                for (m in last.addedMarks) {                  
      163 +                    val c = s.cellAt(m.row, m.col)            
      164 +                    s = s.replaceCell(c.copy(pencilMarks = c.p
          +encilMarks - m.digit))                                        
      165 +                }                                             
      166 +                for (m in last.removedMarks) {                
      167 +                    val c = s.cellAt(m.row, m.col)            
      168 +                    s = s.replaceCell(c.copy(                 
      169 +                        pencilMarks = c.pencilMarks + (m.digit
          + to m.color),                                                 
      170 +                    ))                                        
      171 +                }                                             
      172 +            }                                                 
      173          }
      160 -        return s.replaceCell(restored).copy(history = trimmedH
          -istory)                                                       
      174 +        return s.copy(history = trimmedHistory)               
      175      }
      176  
      177      /** Reset the board to its starting position (givens only)
           . */

● Add the three candidate-sweep reducer methods. Inserting after the
  hidden-single completion:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 106 lines
      362          )
      363      }
      364  
      365 +    // ---- Candidate-sweep hint animation ----               
      366 +                                                              
      367 +    /**                                                       
      368 +     * Enter the candidate-sweep hint animation. Snapshots eve
          +ry existing                                                   
      369 +     * pencil mark so completion can build the consolidated un
          +do move.                                                      
      370 +     */                                                       
      371 +    fun beginCandidateSweep(state: GameState, hint: HintEngine
          +.CandidateSweep): GameState {                                 
      372 +        val priorMarks = buildList {                          
      373 +            for (i in 0 until 81) {                           
      374 +                val c = state.cells[i]                        
      375 +                for ((d, color) in c.pencilMarks) {           
      376 +                    add(AutoClearedMark(c.row, c.col, d, color
          +))                                                            
      377 +                }                                             
      378 +            }                                                 
      379 +        }                                                     
      380 +        return state.copy(                                    
      381 +            phase = GameState.Phase.Hinting,                  
      382 +            activeDigit = null,                               
      383 +            hintUi = HintUi.CandidateSweep(                   
      384 +                initialFill = hint.initialFill,               
      385 +                priorMarks = priorMarks,                      
      386 +            ),                                                
      387 +        )                                                     
      388 +    }                                                         
      389 +                                                              
      390 +    /**                                                       
      391 +     * Seed every empty cell with the full 1..9 candidate set,
          + in the                                                       
      392 +     * default color. No-op when the hint UI isn't a sweep or 
          +when the                                                      
      393 +     * sweep wasn't an initial-fill sweep. Cells that already 
          +carry any                                                     
      394 +     * pencil marks are left alone.                           
      395 +     */                                                       
      396 +    fun applyCandidateSweepFill(state: GameState): GameState {
      397 +        val ui = state.hintUi as? HintUi.CandidateSweep ?: ret
          +urn state                                                     
      398 +        if (!ui.initialFill) return state                     
      399 +        var s = state                                         
      400 +        for (i in 0 until 81) {                               
      401 +            val c = s.cells[i]                                
      402 +            if (c.value != null) continue                     
      403 +            if (c.pencilMarks.isNotEmpty()) continue          
      404 +            s = s.replaceCell(c.copy(pencilMarks = ALL_DIGIT_N
          +OTES))                                                        
      405 +        }                                                     
      406 +        return s                                              
      407 +    }                                                         
      408 +                                                              
      409 +    /**                                                       
      410 +     * Move the sweep cursor. When [currentCell] is non-null, 
          +the cell's                                                    
      411 +     * pencil marks lose every digit in [doomedDigits]. Pass n
          +ull/empty                                                     
      412 +     * for the final settle frame.                            
      413 +     */                                                       
      414 +    fun advanceCandidateSweepCell(                            
      415 +        state: GameState,                                     
      416 +        currentCell: Int?,                                    
      417 +        doomedDigits: Set<Int>,                               
      418 +        eliminatorPeers: Set<Int>,                            
      419 +    ): GameState {                                            
      420 +        val ui = state.hintUi as? HintUi.CandidateSweep ?: ret
          +urn state                                                     
      421 +        var s = state                                         
      422 +        if (currentCell != null && doomedDigits.isNotEmpty()) 
          +{                                                             
      423 +            val c = s.cells[currentCell]                      
      424 +            s = s.replaceCell(c.copy(                         
      425 +                pencilMarks = c.pencilMarks.filterKeys { it !i
          +n doomedDigits },                                             
      426 +            ))                                                
      427 +        }                                                     
      428 +        return s.copy(hintUi = ui.copy(                       
      429 +            currentCell = currentCell,                        
      430 +            eliminatorCells = eliminatorPeers,                
      431 +        ))                                                    
      432 +    }                                                         
      433 +                                                              
      434 +    /**                                                       
      435 +     * Finish the sweep: build the undo move by diffing curren
          +t pencil                                                      
      436 +     * marks against the snapshot, clear hint UI, bump hintCou
          +nt.                                                           
      437 +     */                                                       
      438 +    fun completeCandidateSweep(state: GameState, hint: HintEng
          +ine.CandidateSweep): GameState {                              
      439 +        val ui = state.hintUi as? HintUi.CandidateSweep       
      440 +        val prior = ui?.priorMarks.orEmpty()                  
      441 +                                                              
      442 +        val current = buildList {                             
      443 +            for (i in 0 until 81) {                           
      444 +                val c = state.cells[i]                        
      445 +                for ((d, color) in c.pencilMarks) {           
      446 +                    add(AutoClearedMark(c.row, c.col, d, color
          +))                                                            
      447 +                }                                             
      448 +            }                                                 
      449 +        }                                                     
      450 +                                                              
      451 +        val priorKeys = prior.mapTo(HashSet()) { markKey(it) }
      452 +        val currentKeys = current.mapTo(HashSet()) { markKey(i
          +t) }                                                          
      453 +        val added = current.filter { markKey(it) !in priorKeys
          + }                                                            
      454 +        val removed = prior.filter { markKey(it) !in currentKe
          +ys }                                                          
      455 +                                                              
      456 +        val move = Move.PencilSweep(addedMarks = added, remove
          +dMarks = removed)                                             
      457 +                                                              
      458 +        return state.copy(                                    
      459 +            phase = GameState.Phase.Playing,                  
      460 +            hintUi = null,                                    
      461 +            hintCount = state.hintCount + 1,                  
      462 +            history = state.history + move,                   
      463 +        )                                                     
      464 +    }                                                         
      465 +                                                              
      466 +    private val ALL_DIGIT_NOTES: Map<Int, GuessColor> =       
      467 +        (1..9).associateWith { GuessColor.DEFAULT }           
      468 +                                                              
      469 +    private fun markKey(m: AutoClearedMark): Int = (m.row * 9 
          ++ m.col) * 10 + m.digit                                       
      470 +                                                              
      471      // ---- End-of-game actions ----
      472  
      473      fun beginAutoSolve(state: GameState): GameState =

● Now the animator + skip. Adding a sweepCellMs config knob and threading it
  through:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
       9      val fireworkDuration: Long = 120_000,
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12 +    val sweepCellMs: Long = 150,                               
      13      val showTestButton: Boolean = true,
      14  ) {
      15      companion object {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      36                  fireworkDuration = map["firework_duration"]?.to
          LongOrNull() ?: 120_000,
      37                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      38                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      39 +                sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
         +l() ?: 150,                                                    
      40                  showTestButton = map["show_test_button"]?.toBoo
          leanStrictOrNull() ?: true,
      41              )
      42          }

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 5 lines
      25  #                engine can't find an applicable technique.
      26  hint_flash_ms: 600
      27  
      28 +# sweep_cell_ms: Milliseconds between cells during the Candidat
         +e Sweep                                                        
      29 +#                animation. Sweeps cover many cells per hint so
         + this                                                          
      30 +#                ticks faster than hint_step_ms.               
      31 +sweep_cell_ms: 150                                             
      32 +                                                               
      33  # show_test_button: When true, the main menu shows a "Test" but
          ton below
      34  #                   Extreme that walks through each hint techni
          que's
      35  #                   teaching animation on a pre-built example p
          uzzle.

● Update HintAnimator — add sweep delay, the new run path, and the skip-to-end
  branch:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/game/HintAnimator.kt)
  ⎿  Added 43 lines
      19  class HintAnimator(
      20      private val scope: CoroutineScope,
      21      private val stepDelayMs: () -> Long,
      22 +    private val sweepCellMs: () -> Long,                       
      23      private val getState: () -> GameState,
      24      private val setState: (GameState) -> Unit,
      25  ) {
     ...
      42                  when (hint) {
      43                      is HintEngine.NakedSingle -> runNakedSingle
          (hint)
      44                      is HintEngine.HiddenSingle -> runHiddenSing
          le(hint)
      45 +                    is HintEngine.CandidateSweep -> runCandidat
         +eSweep(hint)                                                   
      46                  }
      47              } finally {
      48                  if (pending === hint) pending = null
     ...
      63          setState(complete(getState(), h))
      64      }
      65  
      66 +    /** Apply every remaining step of [hint] and finish it, ide
         +mpotently. */                                                  
      67      private fun complete(state: GameState, hint: HintEngine.Hin
          t): GameState = when (hint) {
      68          is HintEngine.NakedSingle -> GameReducer.completeNakedS
          ingleHint(state, hint)
      69          is HintEngine.HiddenSingle -> GameReducer.completeHidde
          nSingleHint(state, hint)
      70 +        is HintEngine.CandidateSweep -> {                      
      71 +            var s = state                                      
      72 +            // Re-applying these is idempotent — finished cells
         + skip.                                                         
      73 +            s = GameReducer.applyCandidateSweepFill(s)         
      74 +            for (plan in hint.plan) {                          
      75 +                s = GameReducer.advanceCandidateSweepCell(     
      76 +                    state = s,                                 
      77 +                    currentCell = plan.cellIdx,                
      78 +                    doomedDigits = plan.doomedDigits,          
      79 +                    eliminatorPeers = plan.eliminatorPeers,    
      80 +                )                                              
      81 +            }                                                  
      82 +            GameReducer.completeCandidateSweep(s, hint)        
      83 +        }                                                      
      84      }
      85  
      86      private suspend fun runNakedSingle(hint: HintEngine.NakedSi
          ngle) {
     ...
      119          delay(stepDelayMs())
      120          setState(GameReducer.completeHiddenSingleHint(getState
           (), hint))
      121      }
      122 +                                                              
      123 +    private suspend fun runCandidateSweep(hint: HintEngine.Can
          +didateSweep) {                                                
      124 +        setState(GameReducer.beginCandidateSweep(getState(), h
          +int))                                                         
      125 +        delay(stepDelayMs())                                  
      126 +        if (hint.initialFill) {                               
      127 +            setState(GameReducer.applyCandidateSweepFill(getSt
          +ate()))                                                       
      128 +            delay(stepDelayMs())                              
      129 +        }                                                     
      130 +        for (plan in hint.plan) {                             
      131 +            setState(GameReducer.advanceCandidateSweepCell(   
      132 +                state = getState(),                           
      133 +                currentCell = plan.cellIdx,                   
      134 +                doomedDigits = plan.doomedDigits,             
      135 +                eliminatorPeers = plan.eliminatorPeers,       
      136 +            ))                                                
      137 +            delay(sweepCellMs())                              
      138 +        }                                                     
      139 +        setState(GameReducer.advanceCandidateSweepCell(       
      140 +            state = getState(),                               
      141 +            currentCell = null,                               
      142 +            doomedDigits = emptySet(),                        
      143 +            eliminatorPeers = emptySet(),                     
      144 +        ))                                                    
      145 +        delay(stepDelayMs())                                  
      146 +        setState(GameReducer.completeCandidateSweep(getState()
          +, hint))                                                      
      147 +    }                                                         
      148  }
● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Error editing file

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 1 line
      35      private val animator = HintAnimator(
      36          scope = viewModelScope,
      37          stepDelayMs = { config.hintStepMs },
      38 +        sweepCellMs = { config.sweepCellMs },                  
      39          getState = { _state.value },
      40          setState = { _state.value = it },
      41      )

● Now onHint needs to pass marks so findCandidateSweep sees the player's notes:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 2 lines, removed 1 line
      105          // If the board has any incorrect entries, the engine 
           would reason
      106          // from inconsistent state; flash red instead.
      107          val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      108 -        val hint = if (s.hasIncorrectEntry) null else HintEngi
          -ne.find(grid)                                                 
      108 +        val marks = Array(81) { i -> s.cells[i].pencilMarks.ke
          +ys.toSet() }                                                  
      109 +        val hint = if (s.hasIncorrectEntry) null else HintEngi
          +ne.find(grid, marks)                                          
      110  
      111          if (hint == null) {
      112              _state.value = s.copy(hintUi = HintUi.NoHintFlash)

● Same in TestViewModel:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/TestViewModel.kt)
  ⎿  Added 5 lines, removed 2 lines
      41      private val animator = HintAnimator(
      42          scope = viewModelScope,
      43          stepDelayMs = { config.hintStepMs },
      44 +        sweepCellMs = { config.sweepCellMs },                  
      45          getState = { _state.value },
      46          setState = { _state.value = it },
      47      )
     ...
      52                  _index.value = i
      53                  _state.value = GameState.forNewGame(cases[i].pu
          zzle)
      54                  delay(INTRO_DELAY_MS)
      54 -                val grid = IntArray(81) { idx -> _state.value.c
         -ells[idx].value ?: 0 }                                         
      55 -                val hint: HintEngine.Hint = HintEngine.find(gri
         -d)                                                             
      55 +                val cells = _state.value.cells                 
      56 +                val grid = IntArray(81) { idx -> cells[idx].val
         +ue ?: 0 }                                                      
      57 +                val marks = Array(81) { idx -> cells[idx].penci
         +lMarks.keys.toSet() }                                          
      58 +                val hint: HintEngine.Hint = HintEngine.find(gri
         +d, marks)                                                      
      59                      ?: continue
      60                  animator.launch(hint)?.join()
      61                  delay(SETTLE_DELAY_MS)

● Board needs a CandidateSweep rendering branch and HintLabel needs the new
  case:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 22 lines
      144                          )
      145                      }
      146  
      147 +                    // Candidate-sweep overlay: amber cursor +
          + red                                                          
      148 +                    // eliminator peers for the cell being pru
          +ned.                                                          
      149 +                    if (hint is HintUi.CandidateSweep) {      
      150 +                        for (idx in hint.eliminatorCells) {   
      151 +                            val r = idx / 9; val c = idx % 9  
      152 +                            drawRect(                         
      153 +                                color = eliminatorBg,         
      154 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      155 +                                size = Size(cellW, cellH),    
      156 +                            )                                 
      157 +                        }                                     
      158 +                        val cur = hint.currentCell            
      159 +                        if (cur != null) {                    
      160 +                            val r = cur / 9; val c = cur % 9  
      161 +                            drawRect(                         
      162 +                                color = currentBg,            
      163 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      164 +                                size = Size(cellW, cellH),    
      165 +                            )                                 
      166 +                        }                                     
      167 +                    }                                         
      168 +                                                              
      169                      // Hidden-single overlay: unit frame, curr
           ent cell,
      170                      // outside-unit eliminator peers, and targ
           et. Drawn in
      171                      // order so the target stays on top.

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      90      val text = when (state.hintUi) {
      91          is HintUi.NakedSingle -> "Naked Single"
      92          is HintUi.HiddenSingle -> "Hidden Single"
      93 +        is HintUi.CandidateSweep -> "Candidate Sweep"          
      94          HintUi.NoHintFlash -> "No hint available"
      95          null -> ""
      96      }

● Add a Candidate Sweep test case (first in the list — newest):

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/HintTestCases.kt)
  ⎿  Added 8 lines
      39  
      40      val cases: List<Case> = listOf(
      41          build(
      42 +            title = "Candidate Sweep",                         
      43 +            id = "test-candidate-sweep",                       
      44 +            // Three givens along box 0's diagonal — sparse eno
         +ugh that no                                                    
      45 +            // single applies, but every empty cell in the surr
         +ounding                                                        
      46 +            // rows/cols/box gains eliminations once candidates
         + are seeded.                                                   
      47 +            givenIndices = intArrayOf(0, 1 * 9 + 1, 2 * 9 + 2),
      48 +        ),                                                     
      49 +        build(                                                 
      50              title = "Hidden Single",
      51              id = "test-hidden-single",
      52              // The four 5s pin every cell of box 0 except (0,0)
           — col 1 via

● Now add domain tests for the engine + reducer paths:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 53 lines
      129      )
      130  
      131      @Test
      132 +    fun `Candidate Sweep fires with initialFill when no notes 
          +exist`() {                                                    
      133 +        // Three givens in box 0's diagonal. No naked singles,
          + no hidden                                                    
      134 +        // singles — but seeded candidates would lose lots of 
          +marks.                                                        
      135 +        val grid = IntArray(81)                               
      136 +        grid[0] = 5                                           
      137 +        grid[1 * 9 + 1] = 7                                   
      138 +        grid[2 * 9 + 2] = 8                                   
      139 +                                                              
      140 +        val hint = HintEngine.find(grid)                      
      141 +        assertTrue(hint is HintEngine.CandidateSweep, "expecte
          +d CandidateSweep, got $hint")                                 
      142 +        hint as HintEngine.CandidateSweep                     
      143 +        assertTrue(hint.initialFill)                          
      144 +        assertTrue(hint.plan.isNotEmpty())                    
      145 +        // Sanity: (0,1) should lose 5 (row 0), 7 (col 1 + box
          + 0), 8 (box 0).                                               
      146 +        val planForCell = hint.plan.firstOrNull { it.cellIdx =
          += 1 }                                                         
      147 +        assertNotNull(planForCell)                            
      148 +        assertEquals(setOf(5, 7, 8), planForCell!!.doomedDigit
          +s)                                                            
      149 +    }                                                         
      150 +                                                              
      151 +    @Test                                                     
      152 +    fun `Candidate Sweep returns null when notes exist and not
          +hing to prune`() {                                            
      153 +        // Three givens; pretend the player already has valid 
          +candidates                                                    
      154 +        // everywhere (each empty cell has exactly the digits 
          +not blocked                                                   
      155 +        // by a peer). Then there's no pruning work.          
      156 +        val grid = IntArray(81)                               
      157 +        grid[0] = 5                                           
      158 +        grid[1 * 9 + 1] = 7                                   
      159 +        grid[2 * 9 + 2] = 8                                   
      160 +        val marks = Array(81) { idx ->                        
      161 +            if (grid[idx] != 0) {                             
      162 +                emptySet()                                    
      163 +            } else {                                          
      164 +                val r = idx / 9; val c = idx % 9              
      165 +                (1..9).filter { SudokuSolver.isValidPlacement(
          +grid, r, c, it) }.toSet()                                     
      166 +            }                                                 
      167 +        }                                                     
      168 +        // Naked/hidden singles still don't apply here (every 
          +cell has many                                                 
      169 +        // candidates), so find() falls through to CandidateSw
          +eep — which                                                   
      170 +        // should now report null because there's nothing to r
          +emove.                                                        
      171 +        assertNull(HintEngine.find(grid, marks))              
      172 +    }                                                         
      173 +                                                              
      174 +    @Test                                                     
      175 +    fun `Test-menu Candidate Sweep grid yields a CandidateSwee
          +p`() {                                                        
      176 +        val solution = CANONICAL_SOLUTION                     
      177 +        val givenIndices = intArrayOf(0, 1 * 9 + 1, 2 * 9 + 2)
      178 +        val grid = IntArray(81)                               
      179 +        for (idx in givenIndices) grid[idx] = solution[idx]   
      180 +        val hint = HintEngine.find(grid)                      
      181 +        assertTrue(hint is HintEngine.CandidateSweep)         
      182 +    }                                                         
      183 +                                                              
      184 +    @Test                                                     
      185      fun `returns null on a fully solved grid`() {
      186          assertNull(HintEngine.find(CANONICAL_SOLUTION))
      187      }

● Add a reducer flow test (begin → fill → sweep → complete → undo):

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 57 lines
      544      }
      545  
      546      @Test
      547 +    fun `Candidate Sweep with initial fill seeds notes, prunes
          +, and undoes atomically`() {                                  
      548 +        // Three givens scattered in box 0; no naked or hidden
          + singles.                                                     
      549 +        val grid = IntArray(81)                               
      550 +        grid[0] = 5                                           
      551 +        grid[1 * 9 + 1] = 7                                   
      552 +        grid[2 * 9 + 2] = 8                                   
      553 +        val solution = SudokuSolver.solveOne(grid)            
      554 +            ?: error("setup grid must be solvable")           
      555 +        val fake = Puzzle(                                    
      556 +            id = "cs-flow",                                   
      557 +            difficulty = Difficulty.Beginner,                 
      558 +            givens = grid.toList(),                           
      559 +            solution = solution.toList(),                     
      560 +        )                                                     
      561 +        var s = GameState.forNewGame(fake)                    
      562 +        val hint = HintEngine.find(grid) as HintEngine.Candida
          +teSweep                                                       
      563 +        assertTrue(hint.initialFill)                          
      564 +                                                              
      565 +        s = GameReducer.beginCandidateSweep(s, hint)          
      566 +        assertEquals(GameState.Phase.Hinting, s.phase)        
      567 +                                                              
      568 +        s = GameReducer.applyCandidateSweepFill(s)            
      569 +        // Every empty cell now carries 1..9 in the default co
          +lor.                                                          
      570 +        for (i in 0 until 81) {                               
      571 +            val c = s.cells[i]                                
      572 +            if (c.value != null) continue                     
      573 +            assertEquals((1..9).toSet(), c.pencilMarks.keys, "
          +cell $i should have all candidates")                          
      574 +        }                                                     
      575 +                                                              
      576 +        for (plan in hint.plan) {                             
      577 +            s = GameReducer.advanceCandidateSweepCell(        
      578 +                state = s,                                    
      579 +                currentCell = plan.cellIdx,                   
      580 +                doomedDigits = plan.doomedDigits,             
      581 +                eliminatorPeers = plan.eliminatorPeers,       
      582 +            )                                                 
      583 +        }                                                     
      584 +        // (0,1) lost 5, 7, 8.                                
      585 +        val cell01 = s.cellAt(0, 1)                           
      586 +        assertTrue(5 !in cell01.pencilMarks.keys)             
      587 +        assertTrue(7 !in cell01.pencilMarks.keys)             
      588 +        assertTrue(8 !in cell01.pencilMarks.keys)             
      589 +                                                              
      590 +        s = GameReducer.completeCandidateSweep(s, hint)       
      591 +        assertEquals(GameState.Phase.Playing, s.phase)        
      592 +        assertEquals(1, s.hintCount)                          
      593 +        assertTrue(s.history.last() is Move.PencilSweep)      
      594 +                                                              
      595 +        // Undo: every empty cell loses its candidates again (
          +we started                                                    
      596 +        // with none) and no peer cell carries leftover notes.
      597 +        s = GameReducer.undo(s)                               
      598 +        for (i in 0 until 81) {                               
      599 +            assertTrue(s.cells[i].pencilMarks.isEmpty(), "cell
          + $i should have no notes after undo")                         
      600 +        }                                                     
      601 +    }                                                         
      602 +                                                              
      603 +    @Test                                                     
      604      fun `resetBoard returns to starting position`() {
      605          var s = initial()
      606          s = GameReducer.selectDigit(s, 4)

● solveOne on a 3-clue grid will hang. Reuse a hardcoded valid solution instead:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 16 lines, removed 5 lines
      546      @Test
      547      fun `Candidate Sweep with initial fill seeds notes, prunes
           , and undoes atomically`() {
      548          // Three givens scattered in box 0; no naked or hidden
            singles.
      549 +        // The hardcoded solution avoids SudokuSolver.solveOne
          +, which is                                                    
      550 +        // exponential on such a sparse grid.                 
      551 +        val solution = intArrayOf(                            
      552 +            5, 3, 4, 6, 7, 8, 9, 1, 2,                        
      553 +            6, 7, 2, 1, 9, 5, 3, 4, 8,                        
      554 +            1, 9, 8, 3, 4, 2, 5, 6, 7,                        
      555 +            8, 5, 9, 7, 6, 1, 4, 2, 3,                        
      556 +            4, 2, 6, 8, 5, 3, 7, 9, 1,                        
      557 +            7, 1, 3, 9, 2, 4, 8, 5, 6,                        
      558 +            9, 6, 1, 5, 3, 7, 2, 8, 4,                        
      559 +            2, 8, 7, 4, 1, 9, 6, 3, 5,                        
      560 +            3, 4, 5, 2, 8, 6, 1, 7, 9,                        
      561 +        )                                                     
      562          val grid = IntArray(81)
      550 -        grid[0] = 5                                           
      551 -        grid[1 * 9 + 1] = 7                                   
      552 -        grid[2 * 9 + 2] = 8                                   
      553 -        val solution = SudokuSolver.solveOne(grid)            
      554 -            ?: error("setup grid must be solvable")           
      563 +        grid[0] = solution[0]                 // 5 at (0,0)   
      564 +        grid[1 * 9 + 1] = solution[1 * 9 + 1] // 7 at (1,1)   
      565 +        grid[2 * 9 + 2] = solution[2 * 9 + 2] // 8 at (2,2)   
      566          val fake = Puzzle(
      567              id = "cs-flow",
      568              difficulty = Difficulty.Beginner,

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -20)
  ⎿  HintEngineTest > returns null when no naked single is available() FAIL
     ED
         org.opentest4j.AssertionFailedError at HintEngineTest.kt:47
     … +18 lines (ctrl+o to expand)

● Two cases I need to fix: empty grid should now return CandidateSweep (it's
  useful — fills the candidates); solved grid should still return null (nothing
  to fill).

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 5 lines, removed 3 lines
      165      private fun findCandidateSweep(grid: IntArray, marks: Arra
           y<Set<Int>>?): CandidateSweep? {
      166          val anyMarks = marks != null && marks.any { it.isNotEm
           pty() }
      167          val initialFill = !anyMarks
      168 +        val hasEmptyCells = (0 until 81).any { grid[it] == 0 }
      169  
      170          val plan = mutableListOf<CandidateSweep.CellPlan>()
      171          for (idx in 0 until 81) {
     ...
      192              }
      193          }
      194  
      194 -        // When the board already has notes and nothing to pru
          -ne, there's                                                   
      195 -        // no sweep work — let later techniques try instead.  
      196 -        if (!initialFill && plan.isEmpty()) return null       
      195 +        // No work to do: either there are no empty cells to f
          +ill *and*                                                     
      196 +        // nothing to prune, or notes already exist and nothin
          +g to prune.                                                   
      197 +        val hasFillWork = initialFill && hasEmptyCells        
      198 +        if (plan.isEmpty() && !hasFillWork) return null       
      199          return CandidateSweep(initialFill = initialFill, plan 
           = plan)
      200      }
      201  

● Now update the "no naked single available" test — empty grid now legitimately
  returns CandidateSweep:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 8 lines, removed 3 lines
      41      }
      42  
      43      @Test
      44 -    fun `returns null when no naked single is available`() {   
      45 -        // An empty grid has no naked singles (every cell has 9
         - candidates).                                                  
      44 +    fun `empty grid falls through to a fill-only Candidate Swee
         +p`() {                                                         
      45 +        // No givens, no notes: no single applies, but seeding 
         +candidates                                                     
      46 +        // is itself work — CandidateSweep with initialFill = t
         +rue.                                                           
      47          val grid = IntArray(81)
      47 -        assertNull(HintEngine.find(grid))                      
      48 +        val hint = HintEngine.find(grid)                       
      49 +        assertTrue(hint is HintEngine.CandidateSweep)          
      50 +        hint as HintEngine.CandidateSweep                      
      51 +        assertTrue(hint.initialFill)                           
      52 +        assertTrue(hint.plan.isEmpty(), "no givens means no eli
         +minations")                                                    
      53      }
      54  
      55      @Test
● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -25)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +16 lines (ctrl+o to expand)

● All 41 tests pass. Commit & push:

● Bash(git status)
  ⎿  On branch claude/add-sudoku-hint-button-L1spT
     Your branch is up to date with 'origin/claude/add-sudoku-hint-button-L
     1spT'.
     … +19 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Candidate Sweep technique between Hidden Single and Naked Pair…)
  ⎿  [claude/add-sudoku-hint-button-L1spT f68784d] Add Candidate Sweep tech
     nique between Hidden Single and Naked Pair
      14 files changed, 474 insertions(+), 46 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed ac0baf3.

  Summary of what landed

  - Engine (HintEngine.kt): new CandidateSweep hint variant; find(grid, marks?)
  overload threads pencil marks through; tries singles first, then Candidate
  Sweep. The sealed Hint interface no longer carries targetCell / solutionDigit
  (those were naked-single-specific anyway — subclasses keep them).
  - Reducer (GameReducer.kt): beginCandidateSweep snapshots all current pencil
  marks for undo; applyCandidateSweepFill seeds 1..9 into every empty cell if
  initialFill = true; advanceCandidateSweepCell is the per-cell pruning step;
  completeCandidateSweep diffs current vs snapshot and emits
  Move.PencilSweep(addedMarks, removedMarks). undo was rewritten as a full
  type-switch so it can reverse the new move atomically.
  - Animator (HintAnimator.kt): new runCandidateSweep path; skip() re-applies
  every remaining step idempotently before committing (so cutting mid-sweep
  still produces the same final board).
  - UI: Board renders the amber cursor + red eliminator overlay during the
  sweep. HintLabel shows "Candidate Sweep". HintTestCases gets a new first entry
  (three givens along box 0's diagonal — no singles fire, but every nearby cell
  gains eliminations once candidates are seeded).
  - Config: sweep_cell_ms (default 150) controls per-cell pacing — the sweep
  visits many cells, so it ticks faster than hint_step_ms.
  - Tests: 4 new domain tests cover (1) find returns a fill-only sweep on an
  empty grid, (2) returns a real plan on the test-menu grid, (3) returns null
  when notes are valid and exhaustive, (4) the full reducer flow including undo
  restoring the pre-sweep state.

  One caveat to flag: I had to update the old returns null when no naked single 
  is available test — an empty grid now falls through to a fill-only
  CandidateSweep. That's the intended behavior given the new technique, but it's
  a small semantic shift in what find() returns for the "nothing constructive
  to do" case.

❯ Please also create a button to do a candidate sweep so we don't need to use   
  hints for that. Clicking the button will only add notes when there are no     
  notes, but will always remove any additional notes that can be eliminated     
  using candidate sweep                                                         

● Plan: add onSweep plumbing that runs CandidateSweep directly via the existing
  animator, but flag it as "don't count as a hint" so hintCount doesn't
  increment. The button goes in ActionRow between Undo and Hint.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 12 lines, removed 1 line
      162          return null
      163      }
      164  
      165 -    private fun findCandidateSweep(grid: IntArray, marks: Arra
          -y<Set<Int>>?): CandidateSweep? {                              
      165 +    /**                                                       
      166 +     * Public alias for the [CandidateSweep] search so callers
          + that want                                                    
      167 +     * to trigger a sweep without going through the easiest-fi
          +rst [find]                                                    
      168 +     * cascade (e.g., a dedicated Sweep button) can do so dire
          +ctly.                                                         
      169 +     */                                                       
      170 +    fun findCandidateSweep(grid: IntArray, marks: Array<Set<In
          +t>>? = null): CandidateSweep? {                               
      171 +        require(grid.size == 81)                              
      172 +        require(marks == null || marks.size == 81)            
      173 +        return findCandidateSweepInternal(grid, marks)        
      174 +    }                                                         
      175 +                                                              
      176 +    private fun findCandidateSweepInternal(grid: IntArray, mar
          +ks: Array<Set<Int>>?): CandidateSweep? {                      
      177          val anyMarks = marks != null && marks.any { it.isNotEm
           pty() }
      178          val initialFill = !anyMarks
      179          val hasEmptyCells = (0 until 81).any { grid[it] == 0 }

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 1 line, removed 1 line
      92          require(marks == null || marks.size == 81)
      93          findNakedSingle(grid)?.let { return it }
      94          findHiddenSingle(grid)?.let { return it }
      95 -        findCandidateSweep(grid, marks)?.let { return it }     
      95 +        findCandidateSweepInternal(grid, marks)?.let { return i
         +t }                                                            
      96          return null
      97      }
      98  

● Reducer — make completeCandidateSweep accept a countAsHint flag:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 11 lines, removed 3 lines
      433  
      434      /**
      435       * Finish the sweep: build the undo move by diffing curren
           t pencil
      436 -     * marks against the snapshot, clear hint UI, bump hintCou
          -nt.                                                           
      436 +     * marks against the snapshot, clear hint UI, optionally b
          +ump                                                           
      437 +     * hintCount.                                             
      438 +     *                                                        
      439 +     * Set [countAsHint] to false when the sweep was triggered
          + directly                                                     
      440 +     * by the player (e.g., the Sweep button) rather than as a
          + hint.                                                        
      441       */
      438 -    fun completeCandidateSweep(state: GameState, hint: HintEng
          -ine.CandidateSweep): GameState {                              
      442 +    fun completeCandidateSweep(                               
      443 +        state: GameState,                                     
      444 +        hint: HintEngine.CandidateSweep,                      
      445 +        countAsHint: Boolean = true,                          
      446 +    ): GameState {                                            
      447          val ui = state.hintUi as? HintUi.CandidateSweep
      448          val prior = ui?.priorMarks.orEmpty()
      449  
     ...
      466          return state.copy(
      467              phase = GameState.Phase.Playing,
      468              hintUi = null,
      461 -            hintCount = state.hintCount + 1,                  
      469 +            hintCount = if (countAsHint) state.hintCount + 1 e
          +lse state.hintCount,                                          
      470              history = state.history + move,
      471          )
      472      }

● Animator — track countAsHint alongside pending:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 15 lines, removed 5 lines
      25  ) {
      26  
      27      @Volatile private var pending: HintEngine.Hint? = null
      28 +    @Volatile private var pendingCountAsHint: Boolean = true   
      29      private var job: Job? = null
      30  
      31      val isRunning: Boolean get() = pending != null
     ...
      34       * Launch the animation for [hint] and return the [Job] so 
          callers can
      35       * `join()` if they need to await completion. Only one anim
          ation runs
      36       * at a time; calling this while one is in flight is a no-o
          p.
      37 +     *                                                         
      38 +     * Pass [countAsHint] = false when the player triggered the
         + action                                                        
      39 +     * directly (e.g., the Sweep button) so it doesn't bump hin
         +tCount.                                                        
      40       */
      37 -    fun launch(hint: HintEngine.Hint): Job? {                  
      41 +    fun launch(hint: HintEngine.Hint, countAsHint: Boolean = tr
         +ue): Job? {                                                    
      42          if (pending != null) return job
      43          pending = hint
      44 +        pendingCountAsHint = countAsHint                       
      45          val j = scope.launch {
      46              try {
      47                  when (hint) {
      48                      is HintEngine.NakedSingle -> runNakedSingle
          (hint)
      49                      is HintEngine.HiddenSingle -> runHiddenSing
          le(hint)
      45 -                    is HintEngine.CandidateSweep -> runCandidat
         -eSweep(hint)                                                   
      50 +                    is HintEngine.CandidateSweep -> runCandidat
         +eSweep(hint, countAsHint)                                      
      51                  }
      52              } finally {
      53                  if (pending === hint) pending = null
     ...
      63       */
      64      fun skip() {
      65          val h = pending ?: return
      66 +        val count = pendingCountAsHint                         
      67          pending = null
      68          job?.cancel()
      63 -        setState(complete(getState(), h))                      
      69 +        setState(complete(getState(), h, count))               
      70      }
      71  
      72      /** Apply every remaining step of [hint] and finish it, ide
          mpotently. */
      67 -    private fun complete(state: GameState, hint: HintEngine.Hin
         -t): GameState = when (hint) {                                  
      73 +    private fun complete(                                      
      74 +        state: GameState,                                      
      75 +        hint: HintEngine.Hint,                                 
      76 +        countAsHint: Boolean,                                  
      77 +    ): GameState = when (hint) {                               
      78          is HintEngine.NakedSingle -> GameReducer.completeNakedS
          ingleHint(state, hint)
      79          is HintEngine.HiddenSingle -> GameReducer.completeHidde
          nSingleHint(state, hint)
      80          is HintEngine.CandidateSweep -> {
     ...
      89                      eliminatorPeers = plan.eliminatorPeers,
      90                  )
      91              }
      82 -            GameReducer.completeCandidateSweep(s, hint)        
      92 +            GameReducer.completeCandidateSweep(s, hint, countAs
         +Hint = countAsHint)                                            
      93          }
      94      }
      95  

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 2 lines, removed 2 lines
      130          setState(GameReducer.completeHiddenSingleHint(getState
           (), hint))
      131      }
      132  
      133 -    private suspend fun runCandidateSweep(hint: HintEngine.Can
          -didateSweep) {                                                
      133 +    private suspend fun runCandidateSweep(hint: HintEngine.Can
          +didateSweep, countAsHint: Boolean) {                          
      134          setState(GameReducer.beginCandidateSweep(getState(), h
           int))
      135          delay(stepDelayMs())
      136          if (hint.initialFill) {
     ...
      153              eliminatorPeers = emptySet(),
      154          ))
      155          delay(stepDelayMs())
      156 -        setState(GameReducer.completeCandidateSweep(getState()
          -, hint))                                                      
      156 +        setState(GameReducer.completeCandidateSweep(getState()
          +, hint, countAsHint = countAsHint))                           
      157      }
      158  }

● ViewModel — add onSweep:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 13 lines
       91          }
       92      }
       93  
       94 +    fun onSweep() {                                           
       95 +        val s = _state.value                                  
       96 +        if (s.phase != GameState.Phase.Playing) return        
       97 +        if (s.hintUi != null) return                          
       98 +        if (animator.isRunning) return                        
       99 +        if (s.hasIncorrectEntry) return                       
      100 +                                                              
      101 +        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      102 +        val marks = Array(81) { i -> s.cells[i].pencilMarks.ke
          +ys.toSet() }                                                  
      103 +        val sweep = HintEngine.findCandidateSweep(grid, marks)
          + ?: return                                                    
      104 +        animator.launch(sweep, countAsHint = false)           
      105 +    }                                                         
      106 +                                                              
      107      fun onHint() {
      108          val s = _state.value
      109          // Pressing Hint while an animation is in flight skips
            to the

● Wire the button into ActionRow + GameScreen:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 15 lines, removed 4 lines
      177      isSolveReady: Boolean,
      178      onUndo: () -> Unit,
      179      onClear: () -> Unit,
      180 +    onSweep: () -> Unit,                                      
      181      onHint: () -> Unit,
      182      onGiveUpOrSolve: () -> Unit,
      183      modifier: Modifier = Modifier,
      184  ) {
      185      val canUndo = state.history.isNotEmpty() && state.phase ==
            GameState.Phase.Playing
      186      val busy = state.phase != GameState.Phase.Playing
      187 +    val playing = state.phase == GameState.Phase.Playing      
      188      // The hint button stays enabled while a hint is animating
            so pressing
      189      // it again skips the animation to the final placement.
      188 -    val canHint = state.phase == GameState.Phase.Playing ||   
      189 -        state.phase == GameState.Phase.Hinting                
      190 +    val canHint = playing || state.phase == GameState.Phase.Hi
          +nting                                                         
      191  
      192      Row(
      193          modifier = modifier.fillMaxWidth(),
      193 -        horizontalArrangement = Arrangement.spacedBy(8.dp),   
      194 +        horizontalArrangement = Arrangement.spacedBy(6.dp),   
      195      ) {
      196          Button(
      197              onClick = onUndo,
     ...
      201              Text("Undo")
      202          }
      203          Button(
      204 +            onClick = onSweep,                                
      205 +            enabled = playing,                                
      206 +            modifier = Modifier.weight(1f),                   
      207 +            colors = ButtonDefaults.buttonColors(             
      208 +                containerColor = Color(0xFF1565C0),           
      209 +            ),                                                
      210 +        ) {                                                   
      211 +            Text("Sweep")                                     
      212 +        }                                                     
      213 +        Button(                                               
      214              onClick = onHint,
      215              enabled = canHint,
      216              modifier = Modifier.weight(1f),
     ...
      222          }
      223          Button(
      224              onClick = onClear,
      214 -            enabled = state.phase == GameState.Phase.Playing, 
      225 +            enabled = playing,                                
      226              modifier = Modifier.weight(1f),
      227              colors = ButtonDefaults.buttonColors(
      228                  containerColor = MaterialTheme.colorScheme.err
           or,

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      66                      isSolveReady = viewModel.isReadyToSolve,
      67                      onUndo = viewModel::onUndo,
      68                      onClear = viewModel::onClear,
      69 +                    onSweep = viewModel::onSweep,              
      70                      onHint = viewModel::onHint,
      71                      onGiveUpOrSolve = {
      72                          if (viewModel.isReadyToSolve) viewModel
          .onSolve() else viewModel.onGiveUp()

● Add a domain test verifying countAsHint = false leaves hintCount unchanged:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 37 lines
      612      }
      613  
      614      @Test
      615 +    fun `Candidate Sweep with countAsHint=false leaves hintCou
          +nt unchanged`() {                                             
      616 +        // Same grid as the begin-to-complete sweep test.     
      617 +        val solution = intArrayOf(                            
      618 +            5, 3, 4, 6, 7, 8, 9, 1, 2,                        
      619 +            6, 7, 2, 1, 9, 5, 3, 4, 8,                        
      620 +            1, 9, 8, 3, 4, 2, 5, 6, 7,                        
      621 +            8, 5, 9, 7, 6, 1, 4, 2, 3,                        
      622 +            4, 2, 6, 8, 5, 3, 7, 9, 1,                        
      623 +            7, 1, 3, 9, 2, 4, 8, 5, 6,                        
      624 +            9, 6, 1, 5, 3, 7, 2, 8, 4,                        
      625 +            2, 8, 7, 4, 1, 9, 6, 3, 5,                        
      626 +            3, 4, 5, 2, 8, 6, 1, 7, 9,                        
      627 +        )                                                     
      628 +        val grid = IntArray(81)                               
      629 +        grid[0] = solution[0]; grid[10] = solution[10]; grid[2
          +0] = solution[20]                                             
      630 +        val fake = Puzzle(                                    
      631 +            id = "cs-no-count",                               
      632 +            difficulty = Difficulty.Beginner,                 
      633 +            givens = grid.toList(),                           
      634 +            solution = solution.toList(),                     
      635 +        )                                                     
      636 +        var s = GameState.forNewGame(fake)                    
      637 +        val hint = HintEngine.findCandidateSweep(grid)        
      638 +            ?: error("expected a sweep on this grid")         
      639 +                                                              
      640 +        s = GameReducer.beginCandidateSweep(s, hint)          
      641 +        s = GameReducer.applyCandidateSweepFill(s)            
      642 +        for (plan in hint.plan) {                             
      643 +            s = GameReducer.advanceCandidateSweepCell(s, plan.
          +cellIdx, plan.doomedDigits, plan.eliminatorPeers)             
      644 +        }                                                     
      645 +        s = GameReducer.completeCandidateSweep(s, hint, countA
          +sHint = false)                                                
      646 +        assertEquals(0, s.hintCount)                          
      647 +        // The undo move is still recorded so the player can r
          +evert it.                                                     
      648 +        assertTrue(s.history.last() is Move.PencilSweep)      
      649 +    }                                                         
      650 +                                                              
      651 +    @Test                                                     
      652      fun `resetBoard returns to starting position`() {
      653          var s = initial()
      654          s = GameReducer.selectDigit(s, 4)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● All green. Commit and push.

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add a dedicated Sweep button alongside Hint…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 2f566ec] Add a dedicated Sweep bu
     tton alongside Hint
      7 files changed, 107 insertions(+), 16 deletions(-)
     … +6 lines (ctrl+o to expand)

● Pushed 2f566ec.

  What changed:
  - HintEngine.findCandidateSweep(grid, marks?) is now a public entry point that
  bypasses the singles cascade — find() still uses the existing internal
  version.
  - HintAnimator.launch takes a countAsHint: Boolean = true. The Sweep button
  passes false; existing Hint callers keep the default.
  - GameReducer.completeCandidateSweep(state, hint, countAsHint = true) either
  bumps hintCount or leaves it alone, but always records a Move.PencilSweep so
  undo still reverses the whole thing in one press.
  - GameViewModel.onSweep builds grid+marks, finds the sweep, fires the animator
  with countAsHint = false. No-op if there's nothing to prune or if another
  animation is in flight.
  - ActionRow now has five buttons: Undo | Sweep | Hint | Clear | Give Up. The
  Sweep button is dark blue to distinguish it from the green Hint button;
  spacing dropped from 8dp → 6dp to make room.
[Image #1]

❯ There isn't enough room for the text for each button. Please make it two rows 
  of buttons so there is more space                                             

● Splitting into two rows: Undo/Sweep/Hint on top, Clear/Give Up on the bottom —
  also keeps the destructive actions visually separate.

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 61 lines, removed 51 lines
      189      // it again skips the animation to the final placement.
      190      val canHint = playing || state.phase == GameState.Phase.Hi
           nting
      191  
      192 -    Row(                                                      
      192 +    Column(                                                   
      193          modifier = modifier.fillMaxWidth(),
      194 -        horizontalArrangement = Arrangement.spacedBy(6.dp),   
      194 +        verticalArrangement = Arrangement.spacedBy(6.dp),     
      195      ) {
      196 -        Button(                                               
      197 -            onClick = onUndo,                                 
      198 -            enabled = canUndo,                                
      199 -            modifier = Modifier.weight(1f),                   
      196 +        Row(                                                  
      197 +            modifier = Modifier.fillMaxWidth(),               
      198 +            horizontalArrangement = Arrangement.spacedBy(6.dp)
          +,                                                             
      199          ) {
      201 -            Text("Undo")                                      
      200 +            Button(                                           
      201 +                onClick = onUndo,                             
      202 +                enabled = canUndo,                            
      203 +                modifier = Modifier.weight(1f),               
      204 +            ) {                                               
      205 +                Text("Undo")                                  
      206 +            }                                                 
      207 +            Button(                                           
      208 +                onClick = onSweep,                            
      209 +                enabled = playing,                            
      210 +                modifier = Modifier.weight(1f),               
      211 +                colors = ButtonDefaults.buttonColors(         
      212 +                    containerColor = Color(0xFF1565C0),       
      213 +                ),                                            
      214 +            ) {                                               
      215 +                Text("Sweep")                                 
      216 +            }                                                 
      217 +            Button(                                           
      218 +                onClick = onHint,                             
      219 +                enabled = canHint,                            
      220 +                modifier = Modifier.weight(1f),               
      221 +                colors = ButtonDefaults.buttonColors(         
      222 +                    containerColor = Color(0xFF2E7D32),       
      223 +                ),                                            
      224 +            ) {                                               
      225 +                Text(if (state.phase == GameState.Phase.Hintin
          +g) "Skip" else "Hint")                                        
      226 +            }                                                 
      227          }
      203 -        Button(                                               
      204 -            onClick = onSweep,                                
      205 -            enabled = playing,                                
      206 -            modifier = Modifier.weight(1f),                   
      207 -            colors = ButtonDefaults.buttonColors(             
      208 -                containerColor = Color(0xFF1565C0),           
      209 -            ),                                                
      228 +        Row(                                                  
      229 +            modifier = Modifier.fillMaxWidth(),               
      230 +            horizontalArrangement = Arrangement.spacedBy(6.dp)
          +,                                                             
      231          ) {
      211 -            Text("Sweep")                                     
      232 +            Button(                                           
      233 +                onClick = onClear,                            
      234 +                enabled = playing,                            
      235 +                modifier = Modifier.weight(1f),               
      236 +                colors = ButtonDefaults.buttonColors(         
      237 +                    containerColor = MaterialTheme.colorScheme
          +.error,                                                       
      238 +                ),                                            
      239 +            ) {                                               
      240 +                Text("Clear")                                 
      241 +            }                                                 
      242 +            Button(                                           
      243 +                onClick = onGiveUpOrSolve,                    
      244 +                enabled = !busy,                              
      245 +                modifier = Modifier.weight(1f),               
      246 +                colors = if (isSolveReady) {                  
      247 +                    ButtonDefaults.buttonColors(              
      248 +                        containerColor = MaterialTheme.colorSc
          +heme.primary,                                                 
      249 +                    )                                         
      250 +                } else {                                      
      251 +                    ButtonDefaults.buttonColors(              
      252 +                        containerColor = MaterialTheme.colorSc
          +heme.secondary,                                               
      253 +                    )                                         
      254 +                },                                            
      255 +            ) {                                               
      256 +                Text(if (isSolveReady) "Solve" else "Give Up")
      257 +            }                                                 
      258          }
      213 -        Button(                                               
      214 -            onClick = onHint,                                 
      215 -            enabled = canHint,                                
      216 -            modifier = Modifier.weight(1f),                   
      217 -            colors = ButtonDefaults.buttonColors(             
      218 -                containerColor = Color(0xFF2E7D32),           
      219 -            ),                                                
      220 -        ) {                                                   
      221 -            Text(if (state.phase == GameState.Phase.Hinting) "
          -Skip" else "Hint")                                            
      222 -        }                                                     
      223 -        Button(                                               
      224 -            onClick = onClear,                                
      225 -            enabled = playing,                                
      226 -            modifier = Modifier.weight(1f),                   
      227 -            colors = ButtonDefaults.buttonColors(             
      228 -                containerColor = MaterialTheme.colorScheme.err
          -or,                                                           
      229 -            ),                                                
      230 -        ) {                                                   
      231 -            Text("Clear")                                     
      232 -        }                                                     
      233 -        Button(                                               
      234 -            onClick = onGiveUpOrSolve,                        
      235 -            enabled = !busy,                                  
      236 -            modifier = Modifier.weight(1.3f),                 
      237 -            colors = if (isSolveReady) {                      
      238 -                ButtonDefaults.buttonColors(                  
      239 -                    containerColor = MaterialTheme.colorScheme
          -.primary,                                                     
      240 -                )                                             
      241 -            } else {                                          
      242 -                ButtonDefaults.buttonColors(                  
      243 -                    containerColor = MaterialTheme.colorScheme
          -.secondary,                                                   
      244 -                )                                             
      245 -            },                                                
      246 -        ) {                                                   
      247 -            Text(if (isSolveReady) "Solve" else "Give Up")    
      248 -        }                                                     
      259      }
      260  }

  Searched for 1 pattern (ctrl+o to expand)

● Need to add the Column import:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 1 line
       6  import androidx.compose.foundation.clickable
       7  import androidx.compose.foundation.layout.Arrangement
       8  import androidx.compose.foundation.layout.Box
       9 +import androidx.compose.foundation.layout.Column               
      10  import androidx.compose.foundation.layout.Row
      11  import androidx.compose.foundation.layout.aspectRatio
      12  import androidx.compose.foundation.layout.fillMaxWidth

● Bash(git add -A && git commit -m "Split action buttons into two rows for
      readable labels" && git fetch origin claude/add-sudoku-hint-button-L1spT
      2>&1 | tail -2 && g…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 3af5133] Split action buttons int
     o two rows for readable labels
      1 file changed, 62 insertions(+), 51 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed c706743.

  ActionRow is now a Column with two Rows:
  - Top row (3 buttons, equal weight): Undo | Sweep | Hint
  - Bottom row (2 buttons, equal weight): Clear | Give Up

  Each button gets ~30% / ~50% of the screen width respectively, so labels fit
  on a single line.

❯ When the player hits the hint button, instead of solving a cell it instead    
  highlights the cell that it will cell allowing the player a chance to figure  
  it out. This highlight will remain for 1 minutes. If the hint button is hit a 
  second time while a cell is highlighted it will display the solve method at   
  the top for 1 minute and reset the 1 minute for the cell highlight. If they   
  hit it a third time it will solve the cell. If the player solves the cell     
  (correctly or incorrectly) before the highlight fades then the highlight will 
  stop.                                                                         

● Plan: a three-stage progressive hint sits on GameState (new pendingHint
  field), independent of the animation hintUi. The reducer owns
  set/advance/clear; the ViewModel owns the 60s timer; the Board paints the
  target green when pendingHint is active; the HintLabel shows the technique
  name on stage 2. The first cell-tap on the target wipes it.

  This only applies to single-cell hints (Naked/Hidden Single). Candidate Sweep
  through the Hint button still animates immediately — it has no single target.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameState.kt)
  ⎿  Added 1 line
      10      val phase: Phase = Phase.Playing,
      11      val hintCount: Int = 0,
      12      val hintUi: HintUi? = null,
      13 +    val pendingHint: PendingHint? = null,                      
      14  ) {
      15      enum class Phase { Playing, AutoSolving, Celebrating, Hinti
          ng }
      16  

● Create the PendingHint data class:

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/PendingHint.kt)
  ⎿  Wrote 20 lines to ../../../user/soduku-trainer/domain/src/main/kotlin/
     com/dividetask/sudokutrainer/domain/PendingHint.kt
       2 package com.dividetask.sudokutrainer.domain
       3 /**
         hance tot the player has asked for but hasn't acted on yet — a c
       5  * figure the cell out themselves with progressive disclosure.
       6  *
       8  *  1. [showMethod] = false: the [targetCell] is highlighted, no
         thing else.
         .*  2. [showMethod] = true:  the technique name is also revealed
      10  *  3. *next press* fires the existing animation and clears this
         .
     … +13 lines (ctrl+o to expand)

● Reducer methods + tapCell auto-clear:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 5 lines, removed 1 line
      72              valueColor = CellColor.Guess(state.activeColor),
      73              pencilMarks = emptyMap(),
      74          )
      75 -        return s.replaceCell(newCell).appendHistory(move)      
      75 +        // Placing a value in the pending-hint target cell canc
         +els the hint.                                                  
      76 +        val cleared =                                          
      77 +            if (s.pendingHint?.targetCell == cell.row * 9 + cel
         +l.col) s.copy(pendingHint = null)                              
      78 +            else s                                             
      79 +        return cleared.replaceCell(newCell).appendHistory(move)
      80      }
      81  
      82      private fun tapCellPencilOn(state: GameState, cell: Cell, d
          igit: Int): GameState {

● Now the three reducer entry points for pending hints. Inserting after
  resetBoard:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 21 lines
      182      fun resetBoard(state: GameState): GameState =
      183          GameState.forNewGame(state.puzzle)
      184  
      185 +    // ---- Progressive-hint state ----                       
      186 +                                                              
      187 +    /** Stage 1: cell highlighted, technique name hidden. */  
      188 +    fun setPendingHint(state: GameState, hint: HintEngine.Hint
          +, targetCell: Int): GameState =                               
      189 +        state.copy(pendingHint = PendingHint(                 
      190 +            hint = hint,                                      
      191 +            targetCell = targetCell,                          
      192 +            techniqueName = hint.techniqueName,               
      193 +            showMethod = false,                               
      194 +        ))                                                    
      195 +                                                              
      196 +    /** Stage 2: reveal the technique name; no-op if already s
          +hown or unset. */                                             
      197 +    fun advancePendingHintToMethod(state: GameState): GameStat
          +e {                                                           
      198 +        val ph = state.pendingHint ?: return state            
      199 +        if (ph.showMethod) return state                       
      200 +        return state.copy(pendingHint = ph.copy(showMethod = t
          +rue))                                                         
      201 +    }                                                         
      202 +                                                              
      203 +    fun clearPendingHint(state: GameState): GameState =       
      204 +        if (state.pendingHint == null) state else state.copy(p
          +endingHint = null)                                            
      205 +                                                              
      206      // ---- Hint animation actions ----
      207  
      208      /**

● Config:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12      val sweepCellMs: Long = 150,
      13 +    val pendingHintMs: Long = 60_000,                          
      14      val showTestButton: Boolean = true,
      15  ) {
      16      companion object {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      38                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      39                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      40                  sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
          l() ?: 150,
      41 +                pendingHintMs = map["pending_hint_ms"]?.toLongO
         +rNull() ?: 60_000,                                             
      42                  showTestButton = map["show_test_button"]?.toBoo
          leanStrictOrNull() ?: true,
      43              )
      44          }

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 6 lines
      30  #                ticks faster than hint_step_ms.
      31  sweep_cell_ms: 150
      32  
      33 +# pending_hint_ms: How long the progressive-hint highlight (and
         + the                                                           
      34 +#                  revealed technique name on a second press) s
         +tays on                                                        
      35 +#                  screen before fading. Reset by each subseque
         +nt press                                                       
      36 +#                  that advances the stage.                    
      37 +pending_hint_ms: 60000                                         
      38 +                                                               
      39  # show_test_button: When true, the main menu shows a "Test" but
          ton below
      40  #                   Extreme that walks through each hint techni
          que's
      41  #                   teaching animation on a pre-built example p
          uzzle.

● Now the ViewModel — refactor onHint for the staged flow and add a timer:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 1 line
      12  import com.dividetask.sudokutrainer.domain.GuessColor
      13  import com.dividetask.sudokutrainer.domain.HintEngine
      14  import com.dividetask.sudokutrainer.domain.HintUi
      15 +import kotlinx.coroutines.Job                                  
      16  import kotlinx.coroutines.delay
      17  import kotlinx.coroutines.flow.MutableStateFlow
      18  import kotlinx.coroutines.flow.StateFlow

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 47 lines, removed 7 lines
      105          animator.launch(sweep, countAsHint = false)
      106      }
      107  
      108 +    private var pendingHintJob: Job? = null                   
      109 +                                                              
      110      fun onHint() {
      111          val s = _state.value
      112          // Pressing Hint while an animation is in flight skips
            to the
     ...
      118          if (s.phase != GameState.Phase.Playing) return
      119          if (s.hintUi != null) return
      120  
      121 +        // Stage 2 or 3: a pending hint already exists from a 
          +prior press.                                                  
      122 +        val ph = s.pendingHint                                
      123 +        if (ph != null) {                                     
      124 +            if (!ph.showMethod) {                             
      125 +                _state.value = GameReducer.advancePendingHintT
          +oMethod(s)                                                    
      126 +                startPendingHintTimer()                       
      127 +            } else {                                          
      128 +                // Stage 3: clear the pending hint and run the
          + animation.                                                   
      129 +                pendingHintJob?.cancel()                      
      130 +                _state.value = GameReducer.clearPendingHint(s)
      131 +                animator.launch(ph.hint)                      
      132 +            }                                                 
      133 +            return                                            
      134 +        }                                                     
      135 +                                                              
      136 +        // Stage 1: find a new hint.                          
      137          // If the board has any incorrect entries, the engine 
           would reason
      138          // from inconsistent state; flash red instead.
      139          val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      140          val marks = Array(81) { i -> s.cells[i].pencilMarks.ke
           ys.toSet() }
      141          val hint = if (s.hasIncorrectEntry) null else HintEngi
           ne.find(grid, marks)
      142  
      125 -        if (hint == null) {                                   
      126 -            _state.value = s.copy(hintUi = HintUi.NoHintFlash)
      127 -            viewModelScope.launch {                           
      128 -                delay(config.hintFlashMs)                     
      129 -                _state.value = _state.value.copy(hintUi = null
          -)                                                             
      143 +        when (hint) {                                         
      144 +            null -> {                                         
      145 +                _state.value = s.copy(hintUi = HintUi.NoHintFl
          +ash)                                                          
      146 +                viewModelScope.launch {                       
      147 +                    delay(config.hintFlashMs)                 
      148 +                    _state.value = _state.value.copy(hintUi = 
          +null)                                                         
      149 +                }                                             
      150              }
      131 -            return                                            
      151 +            is HintEngine.NakedSingle -> {                    
      152 +                _state.value = GameReducer.setPendingHint(s, h
          +int, hint.targetCell)                                         
      153 +                startPendingHintTimer()                       
      154 +            }                                                 
      155 +            is HintEngine.HiddenSingle -> {                   
      156 +                _state.value = GameReducer.setPendingHint(s, h
          +int, hint.targetCell)                                         
      157 +                startPendingHintTimer()                       
      158 +            }                                                 
      159 +            is HintEngine.CandidateSweep -> {                 
      160 +                // No single target cell, so no progressive fl
          +ow — animate now.                                             
      161 +                animator.launch(hint)                         
      162 +            }                                                 
      163          }
      133 -        animator.launch(hint)                                 
      164      }
      165  
      166 +    private fun startPendingHintTimer() {                     
      167 +        pendingHintJob?.cancel()                              
      168 +        pendingHintJob = viewModelScope.launch {              
      169 +            delay(config.pendingHintMs)                       
      170 +            if (_state.value.pendingHint != null) {           
      171 +                _state.value = GameReducer.clearPendingHint(_s
          +tate.value)                                                   
      172 +            }                                                 
      173 +        }                                                     
      174 +    }                                                         
      175 +                                                              
      176      companion object {
      177          fun factory(repository: PuzzleRepository, config: Game
           Config, difficulty: Difficulty) =
      178              object : ViewModelProvider.Factory {
● Board renders the pending highlight (only when there's no active animation
  overlay):

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 13 lines
      125                          }
      126                      }
      127  
      128 +                    // Progressive-hint target highlight (draw
          +n only when                                                   
      129 +                    // no active animation overlay is competin
          +g).                                                           
      130 +                    val pending = state.pendingHint           
      131 +                    if (hint == null && pending != null) {    
      132 +                        val r = pending.targetCell / 9        
      133 +                        val c = pending.targetCell % 9        
      134 +                        drawRect(                             
      135 +                            color = targetBg,                 
      136 +                            topLeft = Offset(c * cellW, r * ce
          +llH),                                                         
      137 +                            size = Size(cellW, cellH),        
      138 +                        )                                     
      139 +                    }                                         
      140 +                                                              
      141                      // Hint overlays: green target + red elimi
           nator peers.
      142                      if (hint is HintUi.NakedSingle) {
      143                          for (idx in hint.eliminatorCells) {

● HintLabel shows the technique on stage 2:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 2 lines, removed 1 line
       88  
       89  @Composable
       90  private fun HintLabel(state: GameState) {
       91 +    val pending = state.pendingHint                           
       92      val text = when (state.hintUi) {
       93          is HintUi.NakedSingle -> "Naked Single"
       94          is HintUi.HiddenSingle -> "Hidden Single"
       95          is HintUi.CandidateSweep -> "Candidate Sweep"
       96          HintUi.NoHintFlash -> "No hint available"
       96 -        null -> ""                                            
       97 +        null -> if (pending?.showMethod == true) pending.techn
          +iqueName else ""                                              
       98      }
       99      Box(
      100          modifier = Modifier

● Add domain tests for the new flow:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 62 lines
      649      }
      650  
      651      @Test
      652 +    fun `pending hint advances from highlight to method, then 
          +clears`() {                                                   
      653 +        val s0 = initial()                                    
      654 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      655 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      656 +                                                              
      657 +        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          +etCell)                                                       
      658 +        assertEquals(hint.targetCell, s.pendingHint?.targetCel
          +l)                                                            
      659 +        assertEquals(false, s.pendingHint?.showMethod)        
      660 +        assertEquals("Naked Single", s.pendingHint?.techniqueN
          +ame)                                                          
      661 +                                                              
      662 +        s = GameReducer.advancePendingHintToMethod(s)         
      663 +        assertEquals(true, s.pendingHint?.showMethod)         
      664 +                                                              
      665 +        // Second advance is a no-op once method is shown.    
      666 +        val before = s                                        
      667 +        s = GameReducer.advancePendingHintToMethod(s)         
      668 +        assertSame(before, s)                                 
      669 +                                                              
      670 +        s = GameReducer.clearPendingHint(s)                   
      671 +        assertNull(s.pendingHint)                             
      672 +    }                                                         
      673 +                                                              
      674 +    @Test                                                     
      675 +    fun `placing a value in the pending-hint target clears the
          + hint`() {                                                    
      676 +        val s0 = initial()                                    
      677 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      678 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      679 +        val tr = hint.targetCell / 9                          
      680 +        val tc = hint.targetCell % 9                          
      681 +                                                              
      682 +        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          +etCell)                                                       
      683 +        // Place any digit (correct or not) — the highlight sh
          +ould clear.                                                   
      684 +        val wrong = if (hint.solutionDigit == 1) 2 else 1     
      685 +        s = GameReducer.selectDigit(s, wrong)                 
      686 +        s = GameReducer.tapCell(s, tr, tc)                    
      687 +        assertEquals(wrong, s.cellAt(tr, tc).value)           
      688 +        assertNull(s.pendingHint)                             
      689 +    }                                                         
      690 +                                                              
      691 +    @Test                                                     
      692 +    fun `placing a value in a different cell does not clear a 
          +pending hint`() {                                             
      693 +        val s0 = initial()                                    
      694 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      695 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      696 +        val tr = hint.targetCell / 9                          
      697 +        val tc = hint.targetCell % 9                          
      698 +        // Pick any non-given cell that is not the target.    
      699 +        var otherIdx = -1                                     
      700 +        for (i in 0 until 81) {                               
      701 +            if (i == hint.targetCell) continue                
      702 +            if (!s0.cells[i].given && s0.cells[i].value == nul
          +l) { otherIdx = i; break }                                    
      703 +        }                                                     
      704 +        check(otherIdx != -1)                                 
      705 +        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          +etCell)                                                       
      706 +        s = GameReducer.selectDigit(s, 1)                     
      707 +        s = GameReducer.tapCell(s, otherIdx / 9, otherIdx % 9)
      708 +        assertNotNull(s.pendingHint)                          
      709 +        // Sanity: we didn't accidentally touch the target.   
      710 +        assertNull(s.cellAt(tr, tc).value)                    
      711 +    }                                                         
      712 +                                                              
      713 +    @Test                                                     
      714      fun `resetBoard returns to starting position`() {
      715          var s = initial()
      716          s = GameReducer.selectDigit(s, 4)

  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 1 line
      2  
      3  import org.junit.jupiter.api.Assertions.assertEquals
      4  import org.junit.jupiter.api.Assertions.assertFalse
      5 +import org.junit.jupiter.api.Assertions.assertNotNull           
      6  import org.junit.jupiter.api.Assertions.assertNull
      7  import org.junit.jupiter.api.Assertions.assertSame
      8  import org.junit.jupiter.api.Assertions.assertTrue

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● All tests pass. Commit and push.

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Progressive Hint button: highlight, then technique, then solve…)
  ⎿  [claude/add-sudoku-hint-button-L1spT daef2b9] Progressive Hint button:
      highlight, then technique, then solve
      9 files changed, 181 insertions(+), 9 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed daef2b9.

  Behavior:
  - Press 1 (no pending hint): engine returns NakedSingle / HiddenSingle →
  target cell tinted green, 60s timer starts. No technique name yet.
  - Press 2 (pending, method hidden): technique name appears in the header
  strip; 60s timer restarts.
  - Press 3 (pending, method shown): timer is cancelled, pending state cleared,
  the regular animation fires.
  - Cell modification: placing any value in the target cell — correct or not —
  clears the pending hint immediately via the reducer.
  - Timer expires: silent clear via clearPendingHint.
  - Candidate Sweep through Hint button: no progressive flow (no single target
  cell) — animates immediately as before.

  Config: pending_hint_ms (default 60000 ms) in config.yaml. Tests cover the
  three reducer entry points and the auto-clear behavior on target/non-target
  cell taps.

❯ Please layout the next hints that we need in order and your plan of           
  integrating them                                                              

● Remaining techniques, in the order LogicSolver already grades them (with a
  couple grouped where they share an animation pattern):

  1. Naked Pair (level 3) — two cells in a unit share the same pair {a,b};
  eliminate a and b from the unit's other cells.
  2. Hidden Pair (level 4) — two digits in a unit can only go in two cells;
  eliminate every other candidate from those two cells.
  3. Pointing Pair / Box-Line Reduction (level 5) — a digit's candidates in a
  box all lie on one row or column; eliminate that digit from the rest of that
  row/column (or the inverse).
  4. Naked Triple / Hidden Triple (level 6) — three-cell / three-digit
  generalizations of the pair techniques.
  5. X-Wing (level 7) — a digit forms a rectangle across two rows and two
  columns; eliminate it from the other cells of those columns/rows.
  6. Swordfish (level 8) — three-unit generalization of X-Wing.
  7. XY-Wing (level 8) — chain across three bi-value cells; eliminate the shared
  candidate from cells that see both pincers.

  Key architectural shift: every one of these is an elimination hint, not a
  placement hint. None of them fill a cell. That changes three things:

  1. PendingHint.targetCell: Int → keyCells: Set<Int>. The progressive highlight
  needs to mark the pattern (the pair, the rectangle's corners, etc.), not a
  single cell. The "cell modified → cancel" rule generalizes to "any value
  placed in any key cell cancels."
  2. Completion records a Move.PencilSweep, the type we already built for
  Candidate Sweep — addedMarks = empty, removedMarks = whatever the technique 
  prunes. Undo just works.
  3. The animation is "highlight pattern → sweep eliminations" for every
  technique. We can probably consolidate the animator's per-technique runners
  into one generic runEliminationHint(keyCells, plan) after Naked Pair; only the
  finder differs from technique to technique.

  Integration order I'd recommend (not the same as the difficulty order):

  1. Refactor PendingHint and the Board overlay to support multi-cell key sets
  (one PR, no new techniques yet — the existing Naked/Hidden Single hints become
  a single-element keyCells).
  2. Add a generic EliminationHint shape and animator runner, exercised by Naked
  Pair as the first user (one PR).
  3. Hidden Pair — engine only; reuses the runner (one PR).
  4. Pointing Pair / Box-Line Reduction — engine + a small UI tweak to also
  frame the unit being projected from/onto.
  5. Naked Triple / Hidden Triple — engine only, reuses everything.
  6. X-Wing — engine + draw the rectangle's two rows/columns as the "unit
  frame".
  7. Swordfish — engine only; same frame as X-Wing extended to three.
  8. XY-Wing — engine + a chain-style frame (pivot + two pincers), most distinct
  UI.

  Steps 5, 6, 7 should each be small once steps 1–4 land. Naked Pair is the 
  right next ticket — it forces the architectural refactor while still being a
  small, well-understood technique.

  One thing worth deciding up front: do we want each technique to be
  individually toggleable by the player ("only show me hints up to level X"), or
  always-on with the engine picking the easiest? The current architecture is
  the latter — a per-technique toggle would need a small Settings screen.

  Want me to start with step 1 (the refactor + Naked Pair), or split it
  differently?

❯ Always on with the engine picking the easiest. Knock them all out please      

● This is a large amount of code, so I'll work in batches. Plan: refactor
  PendingHint/HintUi to a generic multi-cell elimination shape first, then add
  the eight techniques one after another using a single shared animator +
  reducer path.
  ⎿  Error editing file

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/PendingHint.kt)
  ⎿  Added 5 lines, removed 4 lines
       2  
       3  /**
       4   * A hint the player has asked for but hasn't acted on yet — a 
          chance to
       5 - * figure the cell out themselves with progressive disclosure. 
       5 + * figure the pattern out themselves with progressive disclosur
         +e.                                                             
       6   *
       7   * Stage progression (driven by repeated Hint presses):
       8 - *  1. [showMethod] = false: the [targetCell] is highlighted, n
         -othing else.                                                   
       8 + *  1. [showMethod] = false: [keyCells] are highlighted, nothin
         +g else.                                                        
       9   *  2. [showMethod] = true:  the technique name is also reveale
          d.
      10   *  3. *next press* fires the existing animation and clears thi
          s.
      11   *
      12   * Cleared automatically when the timer expires, when the playe
          r places
      13 - * a value in [targetCell], or when the game phase changes.    
      13 + * a value in any of [keyCells], or when the game phase changes
         +. For                                                          
      14 + * single-cell hints (Naked/Hidden Single) [keyCells] is a sing
         +leton.                                                         
      15   */
      16  data class PendingHint(
      17      val hint: HintEngine.Hint,
      17 -    val targetCell: Int,                                       
      18 +    val keyCells: Set<Int>,                                    
      19      val techniqueName: String,
      20      val showMethod: Boolean = false,
      21  )

● Update the reducer's setPendingHint and tapCell:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 3 lines, removed 3 lines
      184  
      185      // ---- Progressive-hint state ----
      186  
      187 -    /** Stage 1: cell highlighted, technique name hidden. */  
      188 -    fun setPendingHint(state: GameState, hint: HintEngine.Hint
          -, targetCell: Int): GameState =                               
      187 +    /** Stage 1: key cells highlighted, technique name hidden.
          + */                                                           
      188 +    fun setPendingHint(state: GameState, hint: HintEngine.Hint
          +, keyCells: Set<Int>): GameState =                            
      189          state.copy(pendingHint = PendingHint(
      190              hint = hint,
      191 -            targetCell = targetCell,                          
      191 +            keyCells = keyCells,                              
      192              techniqueName = hint.techniqueName,
      193              showMethod = false,
      194          ))

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 3 lines, removed 2 lines
      72              valueColor = CellColor.Guess(state.activeColor),
      73              pencilMarks = emptyMap(),
      74          )
      75 -        // Placing a value in the pending-hint target cell canc
         -els the hint.                                                  
      75 +        // Placing a value in any of the pending-hint key cells
         + cancels it.                                                   
      76 +        val idx = cell.row * 9 + cell.col                      
      77          val cleared =
      77 -            if (s.pendingHint?.targetCell == cell.row * 9 + cel
         -l.col) s.copy(pendingHint = null)                              
      78 +            if (s.pendingHint?.keyCells?.contains(idx) == true)
         + s.copy(pendingHint = null)                                    
      79              else s
      80          return cleared.replaceCell(newCell).appendHistory(move)
      81      }

● Update tests + ViewModel + Board to use the new shape:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 2 lines, removed 2 lines
      655          val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
           }
      656          val hint = HintEngine.find(grid) as HintEngine.NakedSi
           ngle
      657  
      658 -        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          -etCell)                                                       
      659 -        assertEquals(hint.targetCell, s.pendingHint?.targetCel
          -l)                                                            
      658 +        var s = GameReducer.setPendingHint(s0, hint, setOf(hin
          +t.targetCell))                                                
      659 +        assertEquals(setOf(hint.targetCell), s.pendingHint?.ke
          +yCells)                                                       
      660          assertEquals(false, s.pendingHint?.showMethod)
      661          assertEquals("Naked Single", s.pendingHint?.techniqueN
           ame)
      662  

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 2 lines, removed 2 lines
      680          val tr = hint.targetCell / 9
      681          val tc = hint.targetCell % 9
      682  
      683 -        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          -etCell)                                                       
      683 +        var s = GameReducer.setPendingHint(s0, hint, setOf(hin
          +t.targetCell))                                                
      684          // Place any digit (correct or not) — the highlight sh
           ould clear.
      685          val wrong = if (hint.solutionDigit == 1) 2 else 1
      686          s = GameReducer.selectDigit(s, wrong)
     ...
      703              if (!s0.cells[i].given && s0.cells[i].value == nul
           l) { otherIdx = i; break }
      704          }
      705          check(otherIdx != -1)
      706 -        var s = GameReducer.setPendingHint(s0, hint, hint.targ
          -etCell)                                                       
      706 +        var s = GameReducer.setPendingHint(s0, hint, setOf(hin
          +t.targetCell))                                                
      707          s = GameReducer.selectDigit(s, 1)
      708          s = GameReducer.tapCell(s, otherIdx / 9, otherIdx % 9)
      709          assertNotNull(s.pendingHint)
● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 2 lines, removed 2 lines
      149                  }
      150              }
      151              is HintEngine.NakedSingle -> {
      152 -                _state.value = GameReducer.setPendingHint(s, h
          -int, hint.targetCell)                                         
      152 +                _state.value = GameReducer.setPendingHint(s, h
          +int, setOf(hint.targetCell))                                  
      153                  startPendingHintTimer()
      154              }
      155              is HintEngine.HiddenSingle -> {
      156 -                _state.value = GameReducer.setPendingHint(s, h
          -int, hint.targetCell)                                         
      156 +                _state.value = GameReducer.setPendingHint(s, h
          +int, setOf(hint.targetCell))                                  
      157                  startPendingHintTimer()
      158              }
      159              is HintEngine.CandidateSweep -> {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 9 lines, removed 8 lines
      125                          }
      126                      }
      127  
      128 -                    // Progressive-hint target highlight (draw
          -n only when                                                   
      128 +                    // Progressive-hint key-cell highlight (dr
          +awn only when                                                 
      129                      // no active animation overlay is competin
           g).
      130                      val pending = state.pendingHint
      131                      if (hint == null && pending != null) {
      132 -                        val r = pending.targetCell / 9        
      133 -                        val c = pending.targetCell % 9        
      134 -                        drawRect(                             
      135 -                            color = targetBg,                 
      136 -                            topLeft = Offset(c * cellW, r * ce
          -llH),                                                         
      137 -                            size = Size(cellW, cellH),        
      138 -                        )                                     
      132 +                        for (idx in pending.keyCells) {       
      133 +                            val r = idx / 9; val c = idx % 9  
      134 +                            drawRect(                         
      135 +                                color = targetBg,             
      136 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      137 +                                size = Size(cellW, cellH),    
      138 +                            )                                 
      139 +                        }                                     
      140                      }
      141  
      142                      // Hint overlays: green target + red elimi
           nator peers.

● Now the elimination machinery. Add EliminationHint interface, EliminationStep,
  and a generic UI variant.

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 122 lines
       80      }
       81  
       82      /**
       83 +     * Common payload for elimination-style techniques: every 
          +level-3+                                                      
       84 +     * technique highlights a small "pattern" set of cells and
          + erases a                                                     
       85 +     * computed set of digits from other cells. The animator a
          +nd Board                                                      
       86 +     * render any [EliminationHint] uniformly.                
       87 +     *                                                        
       88 +     * - [keyCells] are the cells that *define* the pattern (e
          +.g. the                                                       
       89 +     *   pair, the rectangle corners). They are tinted green t
          +hroughout.                                                    
       90 +     * - [unitCells] optionally frames the scope of the deduct
          +ion (the                                                      
       91 +     *   pair's row, the X-Wing's columns); empty when no sing
          +le frame                                                      
       92 +     *   applies.                                             
       93 +     * - [plan] is the per-cell elimination plan, in iteration
          + order.                                                       
       94 +     */                                                       
       95 +    sealed interface EliminationHint : Hint {                 
       96 +        val keyCells: Set<Int>                                
       97 +        val unitCells: Set<Int>                               
       98 +        val plan: List<EliminationStep>                       
       99 +    }                                                         
      100 +                                                              
      101 +    /**                                                       
      102 +     * One step of an elimination sweep: erase [doomedDigits] 
          +from                                                          
      103 +     * [cellIdx]'s pencil marks. [explainerCells] are the cell
          +s of the                                                      
      104 +     * pattern that prove this elimination — the Board lights 
          +them red                                                      
      105 +     * during the step (typically a subset of [EliminationHint
          +.keyCells]).                                                  
      106 +     */                                                       
      107 +    data class EliminationStep(                               
      108 +        val cellIdx: Int,                                     
      109 +        val doomedDigits: Set<Int>,                           
      110 +        val explainerCells: Set<Int>,                         
      111 +    )                                                         
      112 +                                                              
      113 +    data class NakedPair(                                     
      114 +        override val keyCells: Set<Int>,                      
      115 +        override val unitCells: Set<Int>,                     
      116 +        override val plan: List<EliminationStep>,             
      117 +        val pairDigits: Set<Int>,                             
      118 +    ) : EliminationHint {                                     
      119 +        override val techniqueName: String = "Naked Pair"     
      120 +    }                                                         
      121 +                                                              
      122 +    data class HiddenPair(                                    
      123 +        override val keyCells: Set<Int>,                      
      124 +        override val unitCells: Set<Int>,                     
      125 +        override val plan: List<EliminationStep>,             
      126 +        val pairDigits: Set<Int>,                             
      127 +    ) : EliminationHint {                                     
      128 +        override val techniqueName: String = "Hidden Pair"    
      129 +    }                                                         
      130 +                                                              
      131 +    data class NakedTriple(                                   
      132 +        override val keyCells: Set<Int>,                      
      133 +        override val unitCells: Set<Int>,                     
      134 +        override val plan: List<EliminationStep>,             
      135 +        val tripleDigits: Set<Int>,                           
      136 +    ) : EliminationHint {                                     
      137 +        override val techniqueName: String = "Naked Triple"   
      138 +    }                                                         
      139 +                                                              
      140 +    data class HiddenTriple(                                  
      141 +        override val keyCells: Set<Int>,                      
      142 +        override val unitCells: Set<Int>,                     
      143 +        override val plan: List<EliminationStep>,             
      144 +        val tripleDigits: Set<Int>,                           
      145 +    ) : EliminationHint {                                     
      146 +        override val techniqueName: String = "Hidden Triple"  
      147 +    }                                                         
      148 +                                                              
      149 +    data class PointingPair(                                  
      150 +        override val keyCells: Set<Int>,                      
      151 +        override val unitCells: Set<Int>,                     
      152 +        override val plan: List<EliminationStep>,             
      153 +        val digit: Int,                                       
      154 +    ) : EliminationHint {                                     
      155 +        override val techniqueName: String = "Pointing Pair"  
      156 +    }                                                         
      157 +                                                              
      158 +    data class BoxLineReduction(                              
      159 +        override val keyCells: Set<Int>,                      
      160 +        override val unitCells: Set<Int>,                     
      161 +        override val plan: List<EliminationStep>,             
      162 +        val digit: Int,                                       
      163 +    ) : EliminationHint {                                     
      164 +        override val techniqueName: String = "Box-Line Reducti
          +on"                                                           
      165 +    }                                                         
      166 +                                                              
      167 +    data class XWing(                                         
      168 +        override val keyCells: Set<Int>,                      
      169 +        override val unitCells: Set<Int>,                     
      170 +        override val plan: List<EliminationStep>,             
      171 +        val digit: Int,                                       
      172 +    ) : EliminationHint {                                     
      173 +        override val techniqueName: String = "X-Wing"         
      174 +    }                                                         
      175 +                                                              
      176 +    data class Swordfish(                                     
      177 +        override val keyCells: Set<Int>,                      
      178 +        override val unitCells: Set<Int>,                     
      179 +        override val plan: List<EliminationStep>,             
      180 +        val digit: Int,                                       
      181 +    ) : EliminationHint {                                     
      182 +        override val techniqueName: String = "Swordfish"      
      183 +    }                                                         
      184 +                                                              
      185 +    data class XYWing(                                        
      186 +        override val keyCells: Set<Int>,                      
      187 +        override val unitCells: Set<Int>,                     
      188 +        override val plan: List<EliminationStep>,             
      189 +        val sharedDigit: Int,                                 
      190 +    ) : EliminationHint {                                     
      191 +        override val techniqueName: String = "XY-Wing"        
      192 +    }                                                         
      193 +                                                              
      194 +    /**                                                       
      195       * Search [grid] for the easiest technique that produces a
            placement
      196       * or note pruning. Returns null if no implemented techniq
           ue applies.
      197       *
     ...
      205          findNakedSingle(grid)?.let { return it }
      206          findHiddenSingle(grid)?.let { return it }
      207          findCandidateSweepInternal(grid, marks)?.let { return 
           it }
      208 +        val cands = candidateGrid(grid)                       
      209 +        findNakedPair(grid, cands)?.let { return it }         
      210 +        findHiddenPair(grid, cands)?.let { return it }        
      211 +        findPointingPair(grid, cands)?.let { return it }      
      212 +        findBoxLineReduction(grid, cands)?.let { return it }  
      213 +        findNakedTriple(grid, cands)?.let { return it }       
      214 +        findHiddenTriple(grid, cands)?.let { return it }      
      215 +        findXWing(grid, cands)?.let { return it }             
      216 +        findSwordfish(grid, cands)?.let { return it }         
      217 +        findXYWing(grid, cands)?.let { return it }            
      218          return null
      219      }
      220  

● Now add all the finder implementations + candidateGrid helper. Inserting after
  findCandidateSweepInternal:

  Read 1 file (ctrl+o to expand)

● Insert the level 3–8 finders. This is the big block:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 449 lines
      334  
      335      private val ALL_DIGITS: Set<Int> = (1..9).toSet()
      336  
      337 +    // ---- Level 3+ candidate-based techniques ----          
      338 +                                                              
      339 +    /**                                                       
      340 +     * Per-cell candidate set computed solely from grid values
          +: for each                                                    
      341 +     * empty cell, the digits not yet placed in its row/column
          +/box. Used                                                    
      342 +     * by the pair/triple/wing finders so deductions don't dep
          +end on the                                                    
      343 +     * player's possibly-incomplete pencil marks.             
      344 +     */                                                       
      345 +    private fun candidateGrid(grid: IntArray): Array<Set<Int>>
          + = Array(81) { idx ->                                         
      346 +        if (grid[idx] != 0) {                                 
      347 +            emptySet()                                        
      348 +        } else {                                              
      349 +            val r = idx / 9; val c = idx % 9                  
      350 +            (1..9).filter { SudokuSolver.isValidPlacement(grid
          +, r, c, it) }.toSet()                                         
      351 +        }                                                     
      352 +    }                                                         
      353 +                                                              
      354 +    private fun findNakedPair(grid: IntArray, cands: Array<Set
          +<Int>>): NakedPair? {                                         
      355 +        for (unit in UNITS_ORDERED) {                         
      356 +            val pairCells = unit.cells.filter { grid[it] == 0 
          +&& cands[it].size == 2 }                                      
      357 +            for (i in pairCells.indices) {                    
      358 +                for (j in i + 1 until pairCells.size) {       
      359 +                    val a = pairCells[i]; val b = pairCells[j]
      360 +                    if (cands[a] != cands[b]) continue        
      361 +                    val digits = cands[a]                     
      362 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      363 +                    for (cell in unit.cells) {                
      364 +                        if (cell == a || cell == b || grid[cel
          +l] != 0) continue                                             
      365 +                        val doomed = cands[cell].intersect(dig
          +its)                                                          
      366 +                        if (doomed.isEmpty()) continue        
      367 +                        plan += EliminationStep(              
      368 +                            cellIdx = cell,                   
      369 +                            doomedDigits = doomed,            
      370 +                            explainerCells = setOf(a, b),     
      371 +                        )                                     
      372 +                    }                                         
      373 +                    if (plan.isEmpty()) continue              
      374 +                    return NakedPair(                         
      375 +                        keyCells = setOf(a, b),               
      376 +                        unitCells = unit.cells.toSet(),       
      377 +                        plan = plan,                          
      378 +                        pairDigits = digits,                  
      379 +                    )                                         
      380 +                }                                             
      381 +            }                                                 
      382 +        }                                                     
      383 +        return null                                           
      384 +    }                                                         
      385 +                                                              
      386 +    private fun findHiddenPair(grid: IntArray, cands: Array<Se
          +t<Int>>): HiddenPair? {                                       
      387 +        for (unit in UNITS_ORDERED) {                         
      388 +            val unsolved = unit.cells.filter { grid[it] == 0 }
      389 +            val digitCells: Map<Int, List<Int>> = (1..9).assoc
          +iateWith { d ->                                               
      390 +                unsolved.filter { d in cands[it] }            
      391 +            }                                                 
      392 +            val twoCellDigits = digitCells.filter { (_, cells)
          + -> cells.size == 2 }                                         
      393 +            val digits = twoCellDigits.keys.toList()          
      394 +            for (i in digits.indices) {                       
      395 +                for (j in i + 1 until digits.size) {          
      396 +                    val d1 = digits[i]; val d2 = digits[j]    
      397 +                    if (twoCellDigits[d1] != twoCellDigits[d2]
          +) continue                                                    
      398 +                    val pair = setOf(d1, d2)                  
      399 +                    val keys = twoCellDigits[d1]!!.toSet()    
      400 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      401 +                    for (cell in keys) {                      
      402 +                        val doomed = cands[cell] - pair       
      403 +                        if (doomed.isEmpty()) continue        
      404 +                        // Explainer: the other cells of the u
          +nit where d1/d2 can't go.                                     
      405 +                        val others = unit.cells.filter {      
      406 +                            it != cell && grid[it] == 0 && it 
          +!in keys                                                      
      407 +                        }.toSet()                             
      408 +                        plan += EliminationStep(              
      409 +                            cellIdx = cell,                   
      410 +                            doomedDigits = doomed,            
      411 +                            explainerCells = others,          
      412 +                        )                                     
      413 +                    }                                         
      414 +                    if (plan.isEmpty()) continue              
      415 +                    return HiddenPair(                        
      416 +                        keyCells = keys,                      
      417 +                        unitCells = unit.cells.toSet(),       
      418 +                        plan = plan,                          
      419 +                        pairDigits = pair,                    
      420 +                    )                                         
      421 +                }                                             
      422 +            }                                                 
      423 +        }                                                     
      424 +        return null                                           
      425 +    }                                                         
      426 +                                                              
      427 +    private fun findPointingPair(grid: IntArray, cands: Array<
          +Set<Int>>): PointingPair? {                                   
      428 +        for (boxIdx in 0 until 9) {                           
      429 +            val box = UNITS_ORDERED[boxIdx]                   
      430 +            for (d in 1..9) {                                 
      431 +                val cells = box.cells.filter { grid[it] == 0 &
          +& d in cands[it] }                                            
      432 +                if (cells.size < 2 || cells.size > 3) continue
      433 +                val rows = cells.map { it / 9 }.toSet()       
      434 +                val cols = cells.map { it % 9 }.toSet()       
      435 +                if (rows.size == 1) {                         
      436 +                    val row = rows.first()                    
      437 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      438 +                    for (c in 0 until 9) {                    
      439 +                        val idx = row * 9 + c                 
      440 +                        if (idx in box.cells) continue        
      441 +                        if (grid[idx] == 0 && d in cands[idx])
          + {                                                            
      442 +                            plan += EliminationStep(idx, setOf
          +(d), cells.toSet())                                           
      443 +                        }                                     
      444 +                    }                                         
      445 +                    if (plan.isNotEmpty()) return PointingPair
          +(                                                             
      446 +                        keyCells = cells.toSet(),             
      447 +                        unitCells = box.cells.toSet(),        
      448 +                        plan = plan,                          
      449 +                        digit = d,                            
      450 +                    )                                         
      451 +                }                                             
      452 +                if (cols.size == 1) {                         
      453 +                    val col = cols.first()                    
      454 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      455 +                    for (r in 0 until 9) {                    
      456 +                        val idx = r * 9 + col                 
      457 +                        if (idx in box.cells) continue        
      458 +                        if (grid[idx] == 0 && d in cands[idx])
          + {                                                            
      459 +                            plan += EliminationStep(idx, setOf
          +(d), cells.toSet())                                           
      460 +                        }                                     
      461 +                    }                                         
      462 +                    if (plan.isNotEmpty()) return PointingPair
          +(                                                             
      463 +                        keyCells = cells.toSet(),             
      464 +                        unitCells = box.cells.toSet(),        
      465 +                        plan = plan,                          
      466 +                        digit = d,                            
      467 +                    )                                         
      468 +                }                                             
      469 +            }                                                 
      470 +        }                                                     
      471 +        return null                                           
      472 +    }                                                         
      473 +                                                              
      474 +    private fun findBoxLineReduction(grid: IntArray, cands: Ar
          +ray<Set<Int>>): BoxLineReduction? {                           
      475 +        for (d in 1..9) {                                     
      476 +            for (r in 0 until 9) {                            
      477 +                val rowCells = (0 until 9).map { r * 9 + it } 
      478 +                val cells = rowCells.filter { grid[it] == 0 &&
          + d in cands[it] }                                             
      479 +                if (cells.size < 2 || cells.size > 3) continue
      480 +                val boxes = cells.map { boxOf(it) }.toSet()   
      481 +                if (boxes.size != 1) continue                 
      482 +                val boxIdx = boxes.first()                    
      483 +                val boxCells = UNITS_ORDERED[boxIdx].cells    
      484 +                val plan = mutableListOf<EliminationStep>()   
      485 +                for (idx in boxCells) {                       
      486 +                    if (idx in cells) continue                
      487 +                    if (grid[idx] == 0 && d in cands[idx]) {  
      488 +                        plan += EliminationStep(idx, setOf(d),
          + cells.toSet())                                               
      489 +                    }                                         
      490 +                }                                             
      491 +                if (plan.isNotEmpty()) return BoxLineReduction
          +(                                                             
      492 +                    keyCells = cells.toSet(),                 
      493 +                    unitCells = rowCells.toSet(),             
      494 +                    plan = plan,                              
      495 +                    digit = d,                                
      496 +                )                                             
      497 +            }                                                 
      498 +            for (c in 0 until 9) {                            
      499 +                val colCells = (0 until 9).map { it * 9 + c } 
      500 +                val cells = colCells.filter { grid[it] == 0 &&
          + d in cands[it] }                                             
      501 +                if (cells.size < 2 || cells.size > 3) continue
      502 +                val boxes = cells.map { boxOf(it) }.toSet()   
      503 +                if (boxes.size != 1) continue                 
      504 +                val boxIdx = boxes.first()                    
      505 +                val boxCells = UNITS_ORDERED[boxIdx].cells    
      506 +                val plan = mutableListOf<EliminationStep>()   
      507 +                for (idx in boxCells) {                       
      508 +                    if (idx in cells) continue                
      509 +                    if (grid[idx] == 0 && d in cands[idx]) {  
      510 +                        plan += EliminationStep(idx, setOf(d),
          + cells.toSet())                                               
      511 +                    }                                         
      512 +                }                                             
      513 +                if (plan.isNotEmpty()) return BoxLineReduction
          +(                                                             
      514 +                    keyCells = cells.toSet(),                 
      515 +                    unitCells = colCells.toSet(),             
      516 +                    plan = plan,                              
      517 +                    digit = d,                                
      518 +                )                                             
      519 +            }                                                 
      520 +        }                                                     
      521 +        return null                                           
      522 +    }                                                         
      523 +                                                              
      524 +    private fun findNakedTriple(grid: IntArray, cands: Array<S
          +et<Int>>): NakedTriple? {                                     
      525 +        for (unit in UNITS_ORDERED) {                         
      526 +            val candidates = unit.cells.filter { grid[it] == 0
          + && cands[it].size in 2..3 }                                  
      527 +            if (candidates.size < 3) continue                 
      528 +            for (i in candidates.indices) {                   
      529 +                for (j in i + 1 until candidates.size) {      
      530 +                    for (k in j + 1 until candidates.size) {  
      531 +                        val a = candidates[i]; val b = candida
          +tes[j]; val c = candidates[k]                                 
      532 +                        val union = cands[a] + cands[b] + cand
          +s[c]                                                          
      533 +                        if (union.size != 3) continue         
      534 +                        val plan = mutableListOf<EliminationSt
          +ep>()                                                         
      535 +                        for (cell in unit.cells) {            
      536 +                            if (cell == a || cell == b || cell
          + == c || grid[cell] != 0) continue                            
      537 +                            val doomed = cands[cell].intersect
          +(union)                                                       
      538 +                            if (doomed.isEmpty()) continue    
      539 +                            plan += EliminationStep(cell, doom
          +ed, setOf(a, b, c))                                           
      540 +                        }                                     
      541 +                        if (plan.isEmpty()) continue          
      542 +                        return NakedTriple(                   
      543 +                            keyCells = setOf(a, b, c),        
      544 +                            unitCells = unit.cells.toSet(),   
      545 +                            plan = plan,                      
      546 +                            tripleDigits = union,             
      547 +                        )                                     
      548 +                    }                                         
      549 +                }                                             
      550 +            }                                                 
      551 +        }                                                     
      552 +        return null                                           
      553 +    }                                                         
      554 +                                                              
      555 +    private fun findHiddenTriple(grid: IntArray, cands: Array<
          +Set<Int>>): HiddenTriple? {                                   
      556 +        for (unit in UNITS_ORDERED) {                         
      557 +            val digitCells: Map<Int, List<Int>> = (1..9).assoc
          +iateWith { d ->                                               
      558 +                unit.cells.filter { grid[it] == 0 && d in cand
          +s[it] }                                                       
      559 +            }                                                 
      560 +            val eligible = digitCells.filter { (_, cells) -> c
          +ells.size in 2..3 }.keys.toList()                             
      561 +            if (eligible.size < 3) continue                   
      562 +            for (i in eligible.indices) {                     
      563 +                for (j in i + 1 until eligible.size) {        
      564 +                    for (k in j + 1 until eligible.size) {    
      565 +                        val triple = setOf(eligible[i], eligib
          +le[j], eligible[k])                                           
      566 +                        val union = digitCells[eligible[i]]!! 
          ++                                                             
      567 +                            digitCells[eligible[j]]!! + digitC
          +ells[eligible[k]]!!                                           
      568 +                        val keys = union.toSet()              
      569 +                        if (keys.size != 3) continue          
      570 +                        val plan = mutableListOf<EliminationSt
          +ep>()                                                         
      571 +                        for (cell in keys) {                  
      572 +                            val doomed = cands[cell] - triple 
      573 +                            if (doomed.isEmpty()) continue    
      574 +                            val others = unit.cells.filter {  
      575 +                                it != cell && grid[it] == 0 &&
          + it !in keys                                                  
      576 +                            }.toSet()                         
      577 +                            plan += EliminationStep(cell, doom
          +ed, others)                                                   
      578 +                        }                                     
      579 +                        if (plan.isEmpty()) continue          
      580 +                        return HiddenTriple(                  
      581 +                            keyCells = keys,                  
      582 +                            unitCells = unit.cells.toSet(),   
      583 +                            plan = plan,                      
      584 +                            tripleDigits = triple,            
      585 +                        )                                     
      586 +                    }                                         
      587 +                }                                             
      588 +            }                                                 
      589 +        }                                                     
      590 +        return null                                           
      591 +    }                                                         
      592 +                                                              
      593 +    private fun findXWing(grid: IntArray, cands: Array<Set<Int
          +>>): XWing? {                                                 
      594 +        for (d in 1..9) {                                     
      595 +            // Row-based: two rows where d's candidates lie in
          + exactly the same two columns.                                
      596 +            val rowPos = Array(9) { r ->                      
      597 +                (0 until 9).filter { c -> grid[r * 9 + c] == 0
          + && d in cands[r * 9 + c] }                                   
      598 +            }                                                 
      599 +            for (r1 in 0 until 9) {                           
      600 +                if (rowPos[r1].size != 2) continue            
      601 +                for (r2 in r1 + 1 until 9) {                  
      602 +                    if (rowPos[r2] != rowPos[r1]) continue    
      603 +                    val (c1, c2) = rowPos[r1]                 
      604 +                    val corners = setOf(r1 * 9 + c1, r1 * 9 + 
          +c2, r2 * 9 + c1, r2 * 9 + c2)                                 
      605 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      606 +                    for (r in 0 until 9) {                    
      607 +                        if (r == r1 || r == r2) continue      
      608 +                        for (c in listOf(c1, c2)) {           
      609 +                            val idx = r * 9 + c               
      610 +                            if (grid[idx] == 0 && d in cands[i
          +dx]) {                                                        
      611 +                                plan += EliminationStep(idx, s
          +etOf(d), corners)                                             
      612 +                            }                                 
      613 +                        }                                     
      614 +                    }                                         
      615 +                    if (plan.isNotEmpty()) return XWing(      
      616 +                        keyCells = corners,                   
      617 +                        unitCells = ((0 until 9).map { r1 * 9 
          ++ it } +                                                      
      618 +                            (0 until 9).map { r2 * 9 + it }).t
          +oSet(),                                                       
      619 +                        plan = plan,                          
      620 +                        digit = d,                            
      621 +                    )                                         
      622 +                }                                             
      623 +            }                                                 
      624 +            // Column-based.                                  
      625 +            val colPos = Array(9) { c ->                      
      626 +                (0 until 9).filter { r -> grid[r * 9 + c] == 0
          + && d in cands[r * 9 + c] }                                   
      627 +            }                                                 
      628 +            for (c1 in 0 until 9) {                           
      629 +                if (colPos[c1].size != 2) continue            
      630 +                for (c2 in c1 + 1 until 9) {                  
      631 +                    if (colPos[c2] != colPos[c1]) continue    
      632 +                    val (r1, r2) = colPos[c1]                 
      633 +                    val corners = setOf(r1 * 9 + c1, r1 * 9 + 
          +c2, r2 * 9 + c1, r2 * 9 + c2)                                 
      634 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      635 +                    for (c in 0 until 9) {                    
      636 +                        if (c == c1 || c == c2) continue      
      637 +                        for (r in listOf(r1, r2)) {           
      638 +                            val idx = r * 9 + c               
      639 +                            if (grid[idx] == 0 && d in cands[i
          +dx]) {                                                        
      640 +                                plan += EliminationStep(idx, s
          +etOf(d), corners)                                             
      641 +                            }                                 
      642 +                        }                                     
      643 +                    }                                         
      644 +                    if (plan.isNotEmpty()) return XWing(      
      645 +                        keyCells = corners,                   
      646 +                        unitCells = ((0 until 9).map { it * 9 
          ++ c1 } +                                                      
      647 +                            (0 until 9).map { it * 9 + c2 }).t
          +oSet(),                                                       
      648 +                        plan = plan,                          
      649 +                        digit = d,                            
      650 +                    )                                         
      651 +                }                                             
      652 +            }                                                 
      653 +        }                                                     
      654 +        return null                                           
      655 +    }                                                         
      656 +                                                              
      657 +    private fun findSwordfish(grid: IntArray, cands: Array<Set
          +<Int>>): Swordfish? {                                         
      658 +        for (d in 1..9) {                                     
      659 +            val rowPos = Array(9) { r ->                      
      660 +                (0 until 9).filter { c -> grid[r * 9 + c] == 0
          + && d in cands[r * 9 + c] }                                   
      661 +            }                                                 
      662 +            val eligibleRows = (0 until 9).filter { rowPos[it]
          +.size in 2..3 }                                               
      663 +            if (eligibleRows.size >= 3) {                     
      664 +                for (i in eligibleRows.indices) for (j in i + 
          +1 until eligibleRows.size)                                    
      665 +                    for (k in j + 1 until eligibleRows.size) {
      666 +                        val rs = listOf(eligibleRows[i], eligi
          +bleRows[j], eligibleRows[k])                                  
      667 +                        val cols = (rowPos[rs[0]] + rowPos[rs[
          +1]] + rowPos[rs[2]]).toSet()                                  
      668 +                        if (cols.size != 3) continue          
      669 +                        val keys = buildSet {                 
      670 +                            for (r in rs) for (c in cols) {   
      671 +                                val idx = r * 9 + c           
      672 +                                if (grid[idx] == 0 && d in can
          +ds[idx]) add(idx)                                             
      673 +                            }                                 
      674 +                        }                                     
      675 +                        val plan = mutableListOf<EliminationSt
          +ep>()                                                         
      676 +                        for (c in cols) {                     
      677 +                            for (r in 0 until 9) {            
      678 +                                if (r in rs) continue         
      679 +                                val idx = r * 9 + c           
      680 +                                if (grid[idx] == 0 && d in can
          +ds[idx]) {                                                    
      681 +                                    plan += EliminationStep(id
          +x, setOf(d), keys)                                            
      682 +                                }                             
      683 +                            }                                 
      684 +                        }                                     
      685 +                        if (plan.isNotEmpty()) return Swordfis
          +h(                                                            
      686 +                            keyCells = keys,                  
      687 +                            unitCells = rs.flatMap { r -> (0 u
          +ntil 9).map { r * 9 + it } }.toSet(),                         
      688 +                            plan = plan,                      
      689 +                            digit = d,                        
      690 +                        )                                     
      691 +                    }                                         
      692 +            }                                                 
      693 +            val colPos = Array(9) { c ->                      
      694 +                (0 until 9).filter { r -> grid[r * 9 + c] == 0
          + && d in cands[r * 9 + c] }                                   
      695 +            }                                                 
      696 +            val eligibleCols = (0 until 9).filter { colPos[it]
          +.size in 2..3 }                                               
      697 +            if (eligibleCols.size >= 3) {                     
      698 +                for (i in eligibleCols.indices) for (j in i + 
          +1 until eligibleCols.size)                                    
      699 +                    for (k in j + 1 until eligibleCols.size) {
      700 +                        val cs = listOf(eligibleCols[i], eligi
          +bleCols[j], eligibleCols[k])                                  
      701 +                        val rows = (colPos[cs[0]] + colPos[cs[
          +1]] + colPos[cs[2]]).toSet()                                  
      702 +                        if (rows.size != 3) continue          
      703 +                        val keys = buildSet {                 
      704 +                            for (c in cs) for (r in rows) {   
      705 +                                val idx = r * 9 + c           
      706 +                                if (grid[idx] == 0 && d in can
          +ds[idx]) add(idx)                                             
      707 +                            }                                 
      708 +                        }                                     
      709 +                        val plan = mutableListOf<EliminationSt
          +ep>()                                                         
      710 +                        for (r in rows) {                     
      711 +                            for (c in 0 until 9) {            
      712 +                                if (c in cs) continue         
      713 +                                val idx = r * 9 + c           
      714 +                                if (grid[idx] == 0 && d in can
          +ds[idx]) {                                                    
      715 +                                    plan += EliminationStep(id
          +x, setOf(d), keys)                                            
      716 +                                }                             
      717 +                            }                                 
      718 +                        }                                     
      719 +                        if (plan.isNotEmpty()) return Swordfis
          +h(                                                            
      720 +                            keyCells = keys,                  
      721 +                            unitCells = cs.flatMap { c -> (0 u
          +ntil 9).map { it * 9 + c } }.toSet(),                         
      722 +                            plan = plan,                      
      723 +                            digit = d,                        
      724 +                        )                                     
      725 +                    }                                         
      726 +            }                                                 
      727 +        }                                                     
      728 +        return null                                           
      729 +    }                                                         
      730 +                                                              
      731 +    private fun findXYWing(grid: IntArray, cands: Array<Set<In
          +t>>): XYWing? {                                               
      732 +        for (pivot in 0 until 81) {                           
      733 +            if (grid[pivot] != 0 || cands[pivot].size != 2) co
          +ntinue                                                        
      734 +            val pc = cands[pivot].toList()                    
      735 +            val a = pc[0]; val b = pc[1]                      
      736 +            val peers = PEERS[pivot]                          
      737 +            val pincerA = mutableListOf<Int>() // {a, c}      
      738 +            val pincerB = mutableListOf<Int>() // {b, c}      
      739 +            for (p in peers) {                                
      740 +                if (grid[p] != 0 || cands[p].size != 2) contin
          +ue                                                            
      741 +                val pcs = cands[p]                            
      742 +                if (a in pcs && b !in pcs) pincerA += p       
      743 +                if (b in pcs && a !in pcs) pincerB += p       
      744 +            }                                                 
      745 +            for (pa in pincerA) {                             
      746 +                val cA = (cands[pa] - a).firstOrNull() ?: cont
          +inue                                                          
      747 +                for (pb in pincerB) {                         
      748 +                    val cB = (cands[pb] - b).firstOrNull() ?: 
          +continue                                                      
      749 +                    if (cA != cB) continue                    
      750 +                    val shared = cA                           
      751 +                    val seeBoth = PEERS[pa].intersect(PEERS[pb
          +])                                                            
      752 +                    val plan = mutableListOf<EliminationStep>(
          +)                                                             
      753 +                    for (cell in seeBoth) {                   
      754 +                        if (cell == pivot || grid[cell] != 0) 
          +continue                                                      
      755 +                        if (shared !in cands[cell]) continue  
      756 +                        plan += EliminationStep(cell, setOf(sh
          +ared), setOf(pa, pb))                                         
      757 +                    }                                         
      758 +                    if (plan.isNotEmpty()) return XYWing(     
      759 +                        keyCells = setOf(pivot, pa, pb),      
      760 +                        unitCells = emptySet(),               
      761 +                        plan = plan,                          
      762 +                        sharedDigit = shared,                 
      763 +                    )                                         
      764 +                }                                             
      765 +            }                                                 
      766 +        }                                                     
      767 +        return null                                           
      768 +    }                                                         
      769 +                                                              
      770 +    private fun boxOf(idx: Int): Int {                        
      771 +        val r = idx / 9; val c = idx % 9                      
      772 +        return (r / 3) * 3 + c / 3                            
      773 +    }                                                         
      774 +                                                              
      775 +    private val PEERS: Array<Set<Int>> = Array(81) { idx ->   
      776 +        val row = idx / 9; val col = idx % 9                  
      777 +        val result = mutableSetOf<Int>()                      
      778 +        for (c in 0 until 9) result.add(row * 9 + c)          
      779 +        for (r in 0 until 9) result.add(r * 9 + col)          
      780 +        val br = (row / 3) * 3; val bc = (col / 3) * 3        
      781 +        for (r in br until br + 3) for (c in bc until bc + 3) 
          +result.add(r * 9 + c)                                         
      782 +        result.remove(idx)                                    
      783 +        result                                                
      784 +    }                                                         
      785 +                                                              
      786      /** Peer cells of [cellIdx] that lie outside [unitCells] a
           nd hold [digit]. */
      787      private fun peersOutsideUnitHolding(
      788          grid: IntArray,

● Now the HintUi.Elimination variant + reducer methods + animator runner + Board
  render + HintLabel. Add the new HintUi variant first:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Added 21 lines
      71          val eliminatorCells: Set<Int> = emptySet(),
      72          val priorMarks: List<AutoClearedMark> = emptyList(),
      73      ) : HintUi
      74 +                                                               
      75 +    /**                                                        
      76 +     * Animation state for any level-3+ elimination technique (
         +Naked /                                                        
      77 +     * Hidden Pair, Triple, Pointing Pair, Box-Line Reduction, 
         +X-Wing,                                                        
      78 +     * Swordfish, XY-Wing). The shape is uniform so the Board a
         +nd the                                                         
      79 +     * animator don't need a separate code path per technique. 
      80 +     *                                                         
      81 +     * - [keyCells] = the pattern cells (green throughout).    
      82 +     * - [unitCells] = optional broader unit frame (pale blue).
      83 +     * - [currentCell] / [explainerCells] = the cell currently 
         +being                                                          
      84 +     *   pruned plus the pattern subset that proves it.        
      85 +     * - [priorMarks] = pre-hint snapshot for the consolidated 
         +undo move.                                                     
      86 +     */                                                        
      87 +    data class Elimination(                                    
      88 +        val techniqueName: String,                             
      89 +        val keyCells: Set<Int>,                                
      90 +        val unitCells: Set<Int> = emptySet(),                  
      91 +        val currentCell: Int? = null,                          
      92 +        val explainerCells: Set<Int> = emptySet(),             
      93 +        val priorMarks: List<AutoClearedMark> = emptyList(),   
      94 +    ) : HintUi                                                 
      95  }

● Add the three reducer methods that drive the elimination animation. Insert
  near the other hint-animation reducers:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 67 lines
      500      private val ALL_DIGIT_NOTES: Map<Int, GuessColor> =
      501          (1..9).associateWith { GuessColor.DEFAULT }
      502  
      503 +    // ---- Generic elimination-hint animation (Naked Pair onw
          +ards) ----                                                    
      504 +                                                              
      505 +    fun beginEliminationHint(state: GameState, hint: HintEngin
          +e.EliminationHint): GameState {                               
      506 +        val priorMarks = buildList {                          
      507 +            for (i in 0 until 81) {                           
      508 +                val c = state.cells[i]                        
      509 +                for ((d, color) in c.pencilMarks) {           
      510 +                    add(AutoClearedMark(c.row, c.col, d, color
          +))                                                            
      511 +                }                                             
      512 +            }                                                 
      513 +        }                                                     
      514 +        return state.copy(                                    
      515 +            phase = GameState.Phase.Hinting,                  
      516 +            activeDigit = null,                               
      517 +            hintUi = HintUi.Elimination(                      
      518 +                techniqueName = hint.techniqueName,           
      519 +                keyCells = hint.keyCells,                     
      520 +                unitCells = hint.unitCells,                   
      521 +                priorMarks = priorMarks,                      
      522 +            ),                                                
      523 +        )                                                     
      524 +    }                                                         
      525 +                                                              
      526 +    fun advanceEliminationStep(                               
      527 +        state: GameState,                                     
      528 +        currentCell: Int?,                                    
      529 +        doomedDigits: Set<Int>,                               
      530 +        explainerCells: Set<Int>,                             
      531 +    ): GameState {                                            
      532 +        val ui = state.hintUi as? HintUi.Elimination ?: return
          + state                                                        
      533 +        var s = state                                         
      534 +        if (currentCell != null && doomedDigits.isNotEmpty()) 
          +{                                                             
      535 +            val c = s.cells[currentCell]                      
      536 +            s = s.replaceCell(c.copy(                         
      537 +                pencilMarks = c.pencilMarks.filterKeys { it !i
          +n doomedDigits },                                             
      538 +            ))                                                
      539 +        }                                                     
      540 +        return s.copy(hintUi = ui.copy(                       
      541 +            currentCell = currentCell,                        
      542 +            explainerCells = explainerCells,                  
      543 +        ))                                                    
      544 +    }                                                         
      545 +                                                              
      546 +    fun completeEliminationHint(state: GameState, hint: HintEn
          +gine.EliminationHint): GameState {                            
      547 +        val ui = state.hintUi as? HintUi.Elimination          
      548 +        val prior = ui?.priorMarks.orEmpty()                  
      549 +        val current = buildList {                             
      550 +            for (i in 0 until 81) {                           
      551 +                val c = state.cells[i]                        
      552 +                for ((d, color) in c.pencilMarks) {           
      553 +                    add(AutoClearedMark(c.row, c.col, d, color
          +))                                                            
      554 +                }                                             
      555 +            }                                                 
      556 +        }                                                     
      557 +        val priorKeys = prior.mapTo(HashSet()) { markKey(it) }
      558 +        val currentKeys = current.mapTo(HashSet()) { markKey(i
          +t) }                                                          
      559 +        val added = current.filter { markKey(it) !in priorKeys
          + }                                                            
      560 +        val removed = prior.filter { markKey(it) !in currentKe
          +ys }                                                          
      561 +        val move = Move.PencilSweep(addedMarks = added, remove
          +dMarks = removed)                                             
      562 +        return state.copy(                                    
      563 +            phase = GameState.Phase.Playing,                  
      564 +            hintUi = null,                                    
      565 +            hintCount = state.hintCount + 1,                  
      566 +            history = state.history + move,                   
      567 +        )                                                     
      568 +    }                                                         
      569 +                                                              
      570      private fun markKey(m: AutoClearedMark): Int = (m.row * 9 
           + m.col) * 10 + m.digit
      571  
      572      // ---- End-of-game actions ----

● Animator: add the elimination runner and dispatch:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 1 line
      48                      is HintEngine.NakedSingle -> runNakedSingle
          (hint)
      49                      is HintEngine.HiddenSingle -> runHiddenSing
          le(hint)
      50                      is HintEngine.CandidateSweep -> runCandidat
          eSweep(hint, countAsHint)
      51 +                    is HintEngine.EliminationHint -> runElimina
         +tionHint(hint)                                                 
      52                  }
      53              } finally {
      54                  if (pending === hint) pending = null

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 12 lines
       92              }
       93              GameReducer.completeCandidateSweep(s, hint, countA
           sHint = countAsHint)
       94          }
       95 +        is HintEngine.EliminationHint -> {                    
       96 +            var s = state                                     
       97 +            for (step in hint.plan) {                         
       98 +                s = GameReducer.advanceEliminationStep(       
       99 +                    state = s,                                
      100 +                    currentCell = step.cellIdx,               
      101 +                    doomedDigits = step.doomedDigits,         
      102 +                    explainerCells = step.explainerCells,     
      103 +                )                                             
      104 +            }                                                 
      105 +            GameReducer.completeEliminationHint(s, hint)      
      106 +        }                                                     
      107      }
      108  
      109      private suspend fun runNakedSingle(hint: HintEngine.NakedS
           ingle) {

● Add the run method:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 22 lines
      143          setState(GameReducer.completeHiddenSingleHint(getState
           (), hint))
      144      }
      145  
      146 +    private suspend fun runEliminationHint(hint: HintEngine.El
          +iminationHint) {                                              
      147 +        setState(GameReducer.beginEliminationHint(getState(), 
          +hint))                                                        
      148 +        delay(stepDelayMs())                                  
      149 +        for (step in hint.plan) {                             
      150 +            setState(GameReducer.advanceEliminationStep(      
      151 +                state = getState(),                           
      152 +                currentCell = step.cellIdx,                   
      153 +                doomedDigits = step.doomedDigits,             
      154 +                explainerCells = step.explainerCells,         
      155 +            ))                                                
      156 +            delay(sweepCellMs())                              
      157 +        }                                                     
      158 +        setState(GameReducer.advanceEliminationStep(          
      159 +            state = getState(),                               
      160 +            currentCell = null,                               
      161 +            doomedDigits = emptySet(),                        
      162 +            explainerCells = emptySet(),                      
      163 +        ))                                                    
      164 +        delay(stepDelayMs())                                  
      165 +        setState(GameReducer.completeEliminationHint(getState(
          +), hint))                                                     
      166 +    }                                                         
      167 +                                                              
      168      private suspend fun runCandidateSweep(hint: HintEngine.Can
           didateSweep, countAsHint: Boolean) {
      169          setState(GameReducer.beginCandidateSweep(getState(), h
           int))
      170          delay(stepDelayMs())

● Update the ViewModel's onHint to recognize EliminationHint and route through
  pending-hint flow with the right keyCells:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 4 lines
      160                  // No single target cell, so no progressive fl
           ow — animate now.
      161                  animator.launch(hint)
      162              }
      163 +            is HintEngine.EliminationHint -> {                
      164 +                _state.value = GameReducer.setPendingHint(s, h
          +int, hint.keyCells)                                           
      165 +                startPendingHintTimer()                       
      166 +            }                                                 
      167          }
      168      }
      169  

● Board renders the elimination overlay:
  ⎿  Error editing file
  ⎿  Error editing file

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 42 lines
      158                          )
      159                      }
      160  
      161 +                    // Generic elimination-hint overlay (pairs
          +, triples,                                                    
      162 +                    // pointing pair, X-Wing, etc.). Order: un
          +it frame                                                      
      163 +                    // (faint), explainer peers (red), current
          + cell                                                         
      164 +                    // (amber), key cells (green) — green stay
          +s on top.                                                     
      165 +                    if (hint is HintUi.Elimination) {         
      166 +                        for (idx in hint.unitCells) {         
      167 +                            if (idx in hint.keyCells) continue
      168 +                            val r = idx / 9; val c = idx % 9  
      169 +                            drawRect(                         
      170 +                                color = unitBg,               
      171 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      172 +                                size = Size(cellW, cellH),    
      173 +                            )                                 
      174 +                        }                                     
      175 +                        for (idx in hint.explainerCells) {    
      176 +                            if (idx in hint.keyCells) continue
      177 +                            val r = idx / 9; val c = idx % 9  
      178 +                            drawRect(                         
      179 +                                color = eliminatorBg,         
      180 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      181 +                                size = Size(cellW, cellH),    
      182 +                            )                                 
      183 +                        }                                     
      184 +                        val cur = hint.currentCell            
      185 +                        if (cur != null && cur !in hint.keyCel
          +ls) {                                                         
      186 +                            val r = cur / 9; val c = cur % 9  
      187 +                            drawRect(                         
      188 +                                color = currentBg,            
      189 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      190 +                                size = Size(cellW, cellH),    
      191 +                            )                                 
      192 +                        }                                     
      193 +                        for (idx in hint.keyCells) {          
      194 +                            val r = idx / 9; val c = idx % 9  
      195 +                            drawRect(                         
      196 +                                color = targetBg,             
      197 +                                topLeft = Offset(c * cellW, r 
          +* cellH),                                                     
      198 +                                size = Size(cellW, cellH),    
      199 +                            )                                 
      200 +                        }                                     
      201 +                    }                                         
      202 +                                                              
      203                      // Candidate-sweep overlay: amber cursor +
            red
      204                      // eliminator peers for the cell being pru
           ned.
      205                      if (hint is HintUi.CandidateSweep) {

● HintLabel uses the techniqueName from the Elimination state:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 3 lines, removed 1 line
       89  @Composable
       90  private fun HintLabel(state: GameState) {
       91      val pending = state.pendingHint
       92 -    val text = when (state.hintUi) {                          
       92 +    val ui = state.hintUi                                     
       93 +    val text = when (ui) {                                    
       94          is HintUi.NakedSingle -> "Naked Single"
       95          is HintUi.HiddenSingle -> "Hidden Single"
       96          is HintUi.CandidateSweep -> "Candidate Sweep"
       97 +        is HintUi.Elimination -> ui.techniqueName             
       98          HintUi.NoHintFlash -> "No hint available"
       99          null -> if (pending?.showMethod == true) pending.techn
           iqueName else ""
      100      }
● Let me run the domain tests to make sure everything compiles and the existing
  tests still pass:

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -30)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +16 lines (ctrl+o to expand)

  Read 1 file (ctrl+o to expand)

● Let me commit this milestone before adding test-menu cases — I want a clean
  rollback point if I break something:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add level 3-8 hint techniques: pairs, triples, pointing, X-Wing,
      swordfish, XY-wing…)
  ⎿  [claude/add-sudoku-hint-button-L1spT d1e5bca] Add level 3-8 hint techn
     iques: pairs, triples, pointing, X-Wing, swordfish, XY-wing
      9 files changed, 769 insertions(+), 24 deletions(-)
     … +8 lines (ctrl+o to expand)

● Now for the test-menu cases. Constructing technique-specific grids by hand is
  error-prone, so I'll write a one-shot search test that prints grids the engine
  actually classifies as each new technique, then hardcode the results.

● Write(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokutr
       ainer/domain/FindTechniqueExamples.kt)
  ⎿  Wrote 66 lines to ../../../user/soduku-trainer/domain/src/test/kotlin/
     com/dividetask/sudokutrainer/domain/FindTechniqueExamples.kt
       2 package com.dividetask.sudokutrainer.domain
       3 import org.junit.jupiter.api.Test
       4 import kotlin.random.Random
       6 /**
       7  * One-shot helper: search for grids whose first applicable Hint
         Engine
         y can betion is each level-3+ technique. Prints the grids so the
       9  * pasted as test-menu fixtures. Not a real test — assertions ab
         sent on
      10  * purpose.
     … +74 lines (ctrl+o to expand)

● Bash(./gradlew :domain:test --tests "*FindTechniqueExamples*"
      -PincludeApp=false --no-daemon -i 2>&1 | grep -E
      "^(NakedPair|HiddenPair|PointingPair|BoxLineReduction|…)
  ⎿  (No output)

  Listed 1 directory (ctrl+o to expand)

● Bash(ps -ef | grep -i "gradle\|java" | grep -v grep | head -5)
  ⎿  (No output)

● Bash(./gradlew :domain:test --tests "*FindTechniqueExamples*"
      -PincludeApp=false --no-daemon -i 2>&1 > /tmp/findex.log; echo "exit=$?";
      wc -l /tmp/findex.log)
  ⎿  Running in the background (↓ to manage)

● Monitor(Wait for example search and emit results)
  ⎿  Monitor started · task bwrrf58e5 · timeout 900s

● While the search runs, prepare HintTestCases to accept pre-filled candidates
  and TestViewModel to apply them:

  Searched for 4 patterns, read 2 files (ctrl+o to expand)

● Background command "Run example finder with explicit log file" completed (exit
code 0)

● Bash(./gradlew :domain:compileTestKotlin -PincludeApp=false --no-daemon 2>&1 |
      tail -30)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +24 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokutr
       ainer/domain/FindTechniqueExamples.kt)
  ⎿  Added 45 lines, removed 33 lines
      24      )
      25  
      26      @Test fun `print examples`() {
      27 -        val targets = listOf(                                  
      28 -            "NakedPair" to { h: HintEngine.Hint -> h is HintEng
         -ine.NakedPair },                                               
      29 -            "HiddenPair" to { h: HintEngine.Hint -> h is HintEn
         -gine.HiddenPair },                                             
      30 -            "PointingPair" to { h: HintEngine.Hint -> h is Hint
         -Engine.PointingPair },                                         
      31 -            "BoxLineReduction" to { h: HintEngine.Hint -> h is 
         -HintEngine.BoxLineReduction },                                 
      32 -            "NakedTriple" to { h: HintEngine.Hint -> h is HintE
         -ngine.NakedTriple },                                           
      33 -            "HiddenTriple" to { h: HintEngine.Hint -> h is Hint
         -Engine.HiddenTriple },                                         
      34 -            "XWing" to { h: HintEngine.Hint -> h is HintEngine.
         -XWing },                                                       
      35 -            "Swordfish" to { h: HintEngine.Hint -> h is HintEng
         -ine.Swordfish },                                               
      36 -            "XYWing" to { h: HintEngine.Hint -> h is HintEngine
         -.XYWing },                                                     
      27 +        val targets = listOf<Pair<String, (HintEngine.Hint) -> 
         +Boolean>>(                                                     
      28 +            "NakedPair" to { h -> h is HintEngine.NakedPair }, 
      29 +            "HiddenPair" to { h -> h is HintEngine.HiddenPair }
         +,                                                              
      30 +            "PointingPair" to { h -> h is HintEngine.PointingPa
         +ir },                                                          
      31 +            "BoxLineReduction" to { h -> h is HintEngine.BoxLin
         +eReduction },                                                  
      32 +            "NakedTriple" to { h -> h is HintEngine.NakedTriple
         + },                                                            
      33 +            "HiddenTriple" to { h -> h is HintEngine.HiddenTrip
         +le },                                                          
      34 +            "XWing" to { h -> h is HintEngine.XWing },         
      35 +            "Swordfish" to { h -> h is HintEngine.Swordfish }, 
      36 +            "XYWing" to { h -> h is HintEngine.XYWing },       
      37          )
      38 -        val rng = Random(0)                                    
      39 -        val found = mutableMapOf<String, IntArray>()           
      40 -        // Try removing increasing numbers of random cells unti
         -l find() reports the target.                                   
      38          for ((name, pred) in targets) {
      42 -            val time0 = System.currentTimeMillis()             
      43 -            outer@ for (kRemoves in 30..70) {                  
      44 -                repeat(2000) {                                 
      45 -                    val grid = solution.copyOf()               
      46 -                    val idxs = (0 until 81).shuffled(rng).take(
         -kRemoves)                                                      
      47 -                    for (i in idxs) grid[i] = 0                
      48 -                    val hint = HintEngine.find(grid) ?: return@
         -repeat                                                         
      49 -                    if (pred(hint)) {                          
      50 -                        found[name] = idxs.sorted().toIntArray(
         -)                                                              
      51 -                        println("$name found with $kRemoves rem
         -ovals: ${idxs.sorted()}")                                      
      52 -                        val rows = (0 until 9).joinToString("\n
         -") { r ->                                                      
      53 -                            (0 until 9).joinToString(", ") { c 
         -->                                                             
      54 -                                grid[r * 9 + c].toString()     
      55 -                            }                                  
      56 -                        }                                      
      57 -                        println(rows)                          
      58 -                        return@outer                           
      59 -                    }                                          
      39 +            val example = searchFor(pred)                      
      40 +            if (example == null) {                             
      41 +                println("$name: NOT FOUND")                    
      42 +            } else {                                           
      43 +                println("$name givens (indices): ${example.toLi
         +st()}")                                                        
      44 +                val grid = solution.copyOf()                   
      45 +                for (i in (0 until 81)) if (i !in example.toSet
         +()) grid[i] = 0                                                
      46 +                println(grid.toList().chunked(9).joinToString("
         +\n") { it.joinToString(", ") })                                
      47 +                println()                                      
      48 +            }                                                  
      49 +        }                                                      
      50 +    }                                                          
      51 +                                                               
      52 +    /** Returns the kept-cell indices, or null if nothing fired
         + within budget. */                                             
      53 +    private fun searchFor(pred: (HintEngine.Hint) -> Boolean): 
         +IntArray? {                                                    
      54 +        val rng = Random(0)                                    
      55 +        val budgetMs = 30_000L                                 
      56 +        val t0 = System.currentTimeMillis()                    
      57 +        var kRemoves = 30                                      
      58 +        while (System.currentTimeMillis() - t0 < budgetMs && kR
         +emoves <= 70) {                                                
      59 +            for (attempt in 0 until 2_000) {                   
      60 +                val grid = solution.copyOf()                   
      61 +                val idxs = (0 until 81).shuffled(rng).take(kRem
         +oves)                                                          
      62 +                for (i in idxs) grid[i] = 0                    
      63 +                // Need marks=non-null so CandidateSweep won't 
         +run with initialFill.                                          
      64 +                val marks = Array(81) { idx ->                 
      65 +                    if (grid[idx] != 0) emptySet() else (1..9).
         +filter {                                                       
      66 +                        SudokuSolver.isValidPlacement(grid, idx
         + / 9, idx % 9, it)                                             
      67 +                    }.toSet()                                  
      68                  }
      61 -                if (System.currentTimeMillis() - time0 > 60_000
         -) break                                                        
      69 +                val hint = HintEngine.find(grid, marks) ?: cont
         +inue                                                           
      70 +                if (pred(hint)) {                              
      71 +                    return (0 until 81).filter { grid[it] != 0 
         +}.toIntArray()                                                 
      72 +                }                                              
      73              }
      63 -            if (name !in found) println("$name: not found")    
      74 +            kRemoves += 5                                      
      75          }
      76 +        return null                                            
      77      }
      78  }

● Bash(./gradlew :domain:test --tests "*FindTechniqueExamples*"
      -PincludeApp=false --no-daemon 2>&1 | tail -10 > /tmp/findex.log; cat
      /tmp/findex.log)
  ⎿  Running in the background (↓ to manage)

  Searched for 2 patterns (ctrl+o to expand)

● Monitor(Wait for FindTechniqueExamples test result)
  ⎿  Monitor started · task bvcwlvvun · timeout 900s

● While the search runs, prepare a candidate-populate helper and TestViewModel
  pre-fill plumbing:
● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 22 lines
      183      fun resetBoard(state: GameState): GameState =
      184          GameState.forNewGame(state.puzzle)
      185  
      186 +    /**                                                       
      187 +     * Fill every empty cell's pencil marks with its valid can
          +didates (the                                                  
      188 +     * digits not blocked by a peer in the row/column/box). Us
          +ed by the                                                     
      189 +     * test menu so puzzles meant to demo pair/triple/wing tec
          +hniques                                                       
      190 +     * start with notes already in place.                     
      191 +     */                                                       
      192 +    fun populateValidCandidates(state: GameState): GameState {
      193 +        var s = state                                         
      194 +        val grid = IntArray(81) { i -> s.cells[i].value ?: 0 }
      195 +        for (i in 0 until 81) {                               
      196 +            val c = s.cells[i]                                
      197 +            if (c.value != null) continue                     
      198 +            val cands = (1..9).filter {                       
      199 +                SudokuSolver.isValidPlacement(grid, c.row, c.c
          +ol, it)                                                       
      200 +            }                                                 
      201 +            s = s.replaceCell(c.copy(                         
      202 +                pencilMarks = cands.associateWith { GuessColor
          +.DEFAULT },                                                   
      203 +            ))                                                
      204 +        }                                                     
      205 +        return s                                              
      206 +    }                                                         
      207 +                                                              
      208      // ---- Progressive-hint state ----
      209  
      210      /** Stage 1: key cells highlighted, technique name hidden.
            */

● Background command "Run example finder" completed (exit code 0)

  Searched for 1 pattern (ctrl+o to expand)

● Monitor event: "Wait for example search and emit results"

● Monitor "Wait for example search and emit results" stream ended

● 7 techniques located; Swordfish and XYWing didn't surface in the budget. Let
  me extend the search for those two in the background while I wire up the rest.

● Write(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokutr
       ainer/domain/FindTechniqueExamples.kt)
  ⎿  Added 8 lines, removed 22 lines
       3  import org.junit.jupiter.api.Test
       4  import kotlin.random.Random
       5  
       6 -/**                                                            
       7 - * One-shot helper: search for grids whose first applicable Hin
         -tEngine                                                        
       8 - * deduction is each level-3+ technique. Prints the grids so th
         -ey can be                                                      
       9 - * pasted as test-menu fixtures. Not a real test — assertions a
         -bsent on                                                       
      10 - * purpose.                                                    
      11 - */                                                            
       6  class FindTechniqueExamples {
       7  
       8      private val solution = intArrayOf(
     ...
      19  
      20      @Test fun `print examples`() {
      21          val targets = listOf<Pair<String, (HintEngine.Hint) -> 
          Boolean>>(
      28 -            "NakedPair" to { h -> h is HintEngine.NakedPair }, 
      29 -            "HiddenPair" to { h -> h is HintEngine.HiddenPair }
         -,                                                              
      30 -            "PointingPair" to { h -> h is HintEngine.PointingPa
         -ir },                                                          
      31 -            "BoxLineReduction" to { h -> h is HintEngine.BoxLin
         -eReduction },                                                  
      32 -            "NakedTriple" to { h -> h is HintEngine.NakedTriple
         - },                                                            
      33 -            "HiddenTriple" to { h -> h is HintEngine.HiddenTrip
         -le },                                                          
      34 -            "XWing" to { h -> h is HintEngine.XWing },         
      22              "Swordfish" to { h -> h is HintEngine.Swordfish },
      23              "XYWing" to { h -> h is HintEngine.XYWing },
      24          )
      25          for ((name, pred) in targets) {
      39 -            val example = searchFor(pred)                      
      26 +            val example = searchFor(pred, budgetMs = 240_000L) 
      27              if (example == null) {
      28                  println("$name: NOT FOUND")
      29              } else {
      30                  println("$name givens (indices): ${example.toLi
          st()}")
      31                  val grid = solution.copyOf()
      45 -                for (i in (0 until 81)) if (i !in example.toSet
         -()) grid[i] = 0                                                
      32 +                val keep = example.toSet()                     
      33 +                for (i in (0 until 81)) if (i !in keep) grid[i]
         + = 0                                                           
      34                  println(grid.toList().chunked(9).joinToString("
          \n") { it.joinToString(", ") })
      35                  println()
      36              }
      37          }
      38      }
      39  
      52 -    /** Returns the kept-cell indices, or null if nothing fired
         - within budget. */                                             
      53 -    private fun searchFor(pred: (HintEngine.Hint) -> Boolean): 
         -IntArray? {                                                    
      40 +    private fun searchFor(pred: (HintEngine.Hint) -> Boolean, b
         +udgetMs: Long): IntArray? {                                    
      41          val rng = Random(0)
      55 -        val budgetMs = 30_000L                                 
      42          val t0 = System.currentTimeMillis()
      43          var kRemoves = 30
      58 -        while (System.currentTimeMillis() - t0 < budgetMs && kR
         -emoves <= 70) {                                                
      59 -            for (attempt in 0 until 2_000) {                   
      44 +        while (System.currentTimeMillis() - t0 < budgetMs && kR
         +emoves <= 75) {                                                
      45 +            for (attempt in 0 until 20_000) {                  
      46 +                if (System.currentTimeMillis() - t0 > budgetMs)
         + return null                                                   
      47                  val grid = solution.copyOf()
      48                  val idxs = (0 until 81).shuffled(rng).take(kRem
          oves)
      49                  for (i in idxs) grid[i] = 0
      63 -                // Need marks=non-null so CandidateSweep won't 
         -run with initialFill.                                          
      50                  val marks = Array(81) { idx ->
      51                      if (grid[idx] != 0) emptySet() else (1..9).
          filter {
      52                          SudokuSolver.isValidPlacement(grid, idx
           / 9, idx % 9, it)
     ...
      57                      return (0 until 81).filter { grid[it] != 0 
          }.toIntArray()
      58                  }
      59              }
      74 -            kRemoves += 5                                      
      60 +            kRemoves += 3                                      
      61          }
      62          return null
      63      }

● Monitor event: "Wait for FindTechniqueExamples test result"

● Monitor "Wait for FindTechniqueExamples test result" stream ended

● Bash(rm /home/user/soduku-trainer/domain/build/test-results/test/TEST-com.divi
      detask.sudokutrainer.domain.FindTechniqueExamples.xml 2>/dev/null;
      ./gradlew :domain:te…)
  ⎿  Running in the background (↓ to manage)

● While that hunts in the background, wire up the 7 found cases into
  HintTestCases + TestViewModel pre-fill:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/test/HintTestCases.kt)
  ⎿  Added 82 lines, removed 18 lines
       8   * technique. Used by the "Test" menu to preview each teaching 
          animation
       9   * in turn. Ordering is newest-technique-first.
      10   *
      11 - * Each case shares the same complete grid as its `solution`; t
         -he                                                             
      12 - * `givens` are a small subset chosen so [com.dividetask.sudoku
         -trainer                                                        
      13 - * .domain.HintEngine.find] reports the intended technique on t
         -he next                                                        
      14 - * deduction. Solutions are hardcoded — no on-device solver cal
         -l —                                                            
      15 - * because backtracking on extremely sparse grids is exponentia
         -l.                                                             
      11 + * The level-3+ cases set [Case.prefillCandidates] = true; the 
         +test                                                           
      12 + * viewmodel then seeds every empty cell with its valid candida
         +tes                                                            
      13 + * before running the hint, which is how those techniques work 
         +on a                                                           
      14 + * real game (after a Candidate Sweep). Solutions are hardcoded
         + — no                                                          
      15 + * on-device solver call — because backtracking on sparse grids
         + is                                                            
      16 + * exponential.                                                
      17   */
      18  object HintTestCases {
      19  
      19 -    data class Case(val title: String, val puzzle: Puzzle)     
      20 +    data class Case(                                           
      21 +        val title: String,                                     
      22 +        val puzzle: Puzzle,                                    
      23 +        val prefillCandidates: Boolean = false,                
      24 +    )                                                          
      25  
      26      /**
      27       * A complete, valid 9x9 grid; every case uses this as its 
          solution.
     ...
       43      )
       44  
       45      val cases: List<Case> = listOf(
       46 +        // Level 8 placeholders go here once we have grids tha
          +t fire them.                                                  
       47          build(
       48 +            title = "X-Wing",                                 
       49 +            id = "test-xwing",                                
       50 +            givenIndices = intArrayOf(                        
       51 +                0, 1, 10, 12, 14, 16, 20, 21, 26, 28, 32,     
       52 +                46, 49, 51, 64, 65, 68, 70, 75, 77, 79,       
       53 +            ),                                                
       54 +            prefill = true,                                   
       55 +        ),                                                    
       56 +        build(                                                
       57 +            title = "Hidden Triple",                          
       58 +            id = "test-hidden-triple",                        
       59 +            givenIndices = intArrayOf(                        
       60 +                6, 7, 13, 23, 37, 40, 42, 43, 51, 65, 69, 70, 
          +71, 72, 74, 79,                                               
       61 +            ),                                                
       62 +            prefill = true,                                   
       63 +        ),                                                    
       64 +        build(                                                
       65 +            title = "Naked Triple",                           
       66 +            id = "test-naked-triple",                         
       67 +            givenIndices = intArrayOf(                        
       68 +                0, 2, 10, 13, 16, 17, 19, 22, 28, 32, 33, 41, 
          +44, 47, 48, 49,                                               
       69 +                54, 62, 63, 74, 78,                           
       70 +            ),                                                
       71 +            prefill = true,                                   
       72 +        ),                                                    
       73 +        build(                                                
       74 +            title = "Box-Line Reduction",                     
       75 +            id = "test-box-line",                             
       76 +            givenIndices = intArrayOf(                        
       77 +                18, 19, 20, 22, 24, 27, 32, 33, 34, 35, 37, 42
          +, 52, 53, 57,                                                 
       78 +                62, 65, 69, 70, 72, 77,                       
       79 +            ),                                                
       80 +            prefill = true,                                   
       81 +        ),                                                    
       82 +        build(                                                
       83 +            title = "Pointing Pair",                          
       84 +            id = "test-pointing-pair",                        
       85 +            givenIndices = intArrayOf(                        
       86 +                3, 4, 6, 10, 14, 24, 29, 33, 36, 37, 38, 43, 4
          +4, 45, 47, 52,                                                
       87 +                53, 56, 65, 66, 67, 68, 69, 71, 78, 80,       
       88 +            ),                                                
       89 +            prefill = true,                                   
       90 +        ),                                                    
       91 +        build(                                                
       92 +            title = "Hidden Pair",                            
       93 +            id = "test-hidden-pair",                          
       94 +            givenIndices = intArrayOf(                        
       95 +                8, 12, 16, 19, 20, 21, 23, 24, 25, 30, 33, 40,
          + 45, 48, 50,                                                  
       96 +                53, 55, 56, 61, 63, 67, 69, 71, 74, 77, 78,   
       97 +            ),                                                
       98 +            prefill = true,                                   
       99 +        ),                                                    
      100 +        build(                                                
      101 +            title = "Naked Pair",                             
      102 +            id = "test-naked-pair",                           
      103 +            givenIndices = intArrayOf(                        
      104 +                7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 
          +46, 47, 52, 53,                                               
      105 +                54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74
          +, 75, 78, 79,                                                 
      106 +            ),                                                
      107 +            prefill = true,                                   
      108 +        ),                                                    
      109 +        build(                                                
      110              title = "Candidate Sweep",
      111              id = "test-candidate-sweep",
       44 -            // Three givens along box 0's diagonal — sparse en
          -ough that no                                                  
       45 -            // single applies, but every empty cell in the sur
          -rounding                                                      
       46 -            // rows/cols/box gains eliminations once candidate
          -s are seeded.                                                 
      112              givenIndices = intArrayOf(0, 1 * 9 + 1, 2 * 9 + 2)
           ,
      113          ),
      114          build(
      115              title = "Hidden Single",
      116              id = "test-hidden-single",
       52 -            // The four 5s pin every cell of box 0 except (0,0
          -) — col 1 via                                                 
       53 -            // (3,1), col 2 via (8,2), row 1 via (1,5), row 2 
          -via (2,6).                                                    
      117              givenIndices = intArrayOf(
       55 -                3 * 9 + 1,                                    
       56 -                1 * 9 + 5,                                    
       57 -                2 * 9 + 6,                                    
       58 -                8 * 9 + 2,                                    
      118 +                3 * 9 + 1, 1 * 9 + 5, 2 * 9 + 6, 8 * 9 + 2,   
      119              ),
      120          ),
      121          build(
      122              title = "Naked Single",
      123              id = "test-naked-single",
       64 -            // Row 0 minus (0,0). The eight clues cover every 
          -digit but                                                     
       65 -            // five, so (0,0) has only `5` as its candidate.  
      124              givenIndices = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
      125          ),
      126      )
      127  
       70 -    private fun build(title: String, id: String, givenIndices:
          - IntArray): Case {                                            
      128 +    private fun build(                                        
      129 +        title: String,                                        
      130 +        id: String,                                           
      131 +        givenIndices: IntArray,                               
      132 +        prefill: Boolean = false,                             
      133 +    ): Case {                                                 
      134          val givens = IntArray(81)
      135          for (idx in givenIndices) givens[idx] = CANONICAL_SOLU
           TION[idx]
      136          return Case(
     ...
      141                  givens = givens.toList(),
      142                  solution = CANONICAL_SOLUTION.toList(),
      143              ),
      144 +            prefillCandidates = prefill,                      
      145          )
      146      }
      147  }

● Wire pre-fill into TestViewModel:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/TestViewModel.kt)
  ⎿  Added 1 line
       5  import androidx.lifecycle.viewModelScope
       6  import androidx.lifecycle.viewmodel.CreationExtras
       7  import com.dividetask.sudokutrainer.data.GameConfig
       8 +import com.dividetask.sudokutrainer.domain.GameReducer         
       9  import com.dividetask.sudokutrainer.domain.GameState
      10  import com.dividetask.sudokutrainer.domain.HintEngine
      11  import com.dividetask.sudokutrainer.ui.game.HintAnimator

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/TestViewModel.kt)
  ⎿  Added 6 lines, removed 1 line
      51          viewModelScope.launch {
      52              for (i in cases.indices) {
      53                  _index.value = i
      54 -                _state.value = GameState.forNewGame(cases[i].pu
         -zzle)                                                          
      54 +                val case = cases[i]                            
      55 +                var s = GameState.forNewGame(case.puzzle)      
      56 +                if (case.prefillCandidates) {                  
      57 +                    s = GameReducer.populateValidCandidates(s) 
      58 +                }                                              
      59 +                _state.value = s                               
      60                  delay(INTRO_DELAY_MS)
      61                  val cells = _state.value.cells
      62                  val grid = IntArray(81) { idx -> cells[idx].val
          ue ?: 0 }

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon --tests
      "*HintEngineTest" --tests "*GameReducerTest" 2>&1 | tail -10)
  ⎿  > Task :domain:classes UP-TO-DATE
     > Task :domain:jar UP-TO-DATE
     > Task :domain:compileTestKotlin UP-TO-DATE
     … +7 lines (ctrl+o to expand)

● Background command "Search Swordfish + XYWing with bigger budget" completed
(exit code 0)

● Check the second-pass search result:

  Searched for 2 patterns, read 1 file (ctrl+o to expand)

● Bash(./gradlew :domain:test --tests "*FindTechniqueExamples*"
      -PincludeApp=false --no-daemon 2>&1 | tail -3; cat
      /home/user/soduku-trainer/domain/build/test-results/…)
  ⎿  Running in the background (↓ to manage)

● Monitor(Stream Swordfish + XYWing search results)
  ⎿  Monitor started · task bn6wztp4w · timeout 1800s

● Let me commit the current progress while the Swordfish search runs:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Test menu cases for Naked/Hidden Pair, Triple, Pointing, Box-Line,
      X-Wing…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 3aa59a0] Add Test menu cases for
     Naked/Hidden Pair, Triple, Pointing, Box-Line, X-Wing
      4 files changed, 175 insertions(+), 19 deletions(-)
     … +8 lines (ctrl+o to expand)

● Background command "Re-run to get search output" completed (exit code 0)

● Monitor event: "Stream Swordfish + XYWing search results"

● Monitor "Stream Swordfish + XYWing search results" stream ended
● XYWing found. Swordfish is genuinely rare (won't surface from random
  hole-poking against one solution); I'll leave its test case as a known
  omission. Adding XYWing now:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/test/HintTestCases.kt)
  ⎿  Added 11 lines, removed 1 line
      43      )
      44  
      45      val cases: List<Case> = listOf(
      46 -        // Level 8 placeholders go here once we have grids that
         - fire them.                                                    
      46          build(
      47 +            title = "XY-Wing",                                 
      48 +            id = "test-xy-wing",                               
      49 +            givenIndices = intArrayOf(                         
      50 +                2, 5, 6, 10, 11, 15, 16, 24, 28, 35, 37, 39, 42
         +, 44, 52, 55,                                                  
      51 +                57, 60, 62, 67, 70, 71, 73, 74, 76, 78, 79,    
      52 +            ),                                                 
      53 +            prefill = true,                                    
      54 +        ),                                                     
      55 +        // Swordfish: no grid yet — the random search didn't fi
         +nd one                                                         
      56 +        // against this canonical solution within the time budg
         +et.                                                            
      57 +        build(                                                 
      58              title = "X-Wing",
      59              id = "test-xwing",
      60              givenIndices = intArrayOf(

● Remove the temporary search test, then run all domain tests to confirm
  everything's green:

● Bash(rm /home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudoku
      trainer/domain/FindTechniqueExamples.kt && ./gradlew :domain:test
      -PincludeApp=false --…)
  ⎿  > Task :domain:classes UP-TO-DATE
     > Task :domain:jar UP-TO-DATE
     > Task :domain:processTestResources NO-SOURCE
     … +7 lines (ctrl+o to expand)

● All tests pass. Commit and push:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add XY-Wing test case; remove one-shot example-finder test…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 95fb076] Add XY-Wing test case; r
     emove one-shot example-finder test
      2 files changed, 11 insertions(+), 65 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed 95fb076. Summary of what's now live on the branch:

  Engine (HintEngine.kt): eight new technique types behind a shared
  EliminationHint interface — NakedPair, HiddenPair, PointingPair,
  BoxLineReduction, NakedTriple, HiddenTriple, XWing, Swordfish, XYWing. Cascade
  order: singles → Candidate Sweep → pair → pointing → box-line → triple →
  X-Wing → Swordfish → XY-Wing.

  Reducer / animator / Board: one beginEliminationHint / advanceEliminationStep
  / completeEliminationHint trio drives every level-3+ animation.
  Move.PencilSweep handles undo for all of them. Board paints green key cells +
  optional pale-blue unit frame + amber sweep cursor + red explainer peers.

  Progressive Hint: PendingHint.keyCells: Set<Int> already supported multi-cell
  patterns from last refactor — all 8 techniques light up their pattern cells on
  first press, show the technique name on second, animate on third.

  Test menu: eight of nine new cases wired up with hardcoded grids found by a
  temporary random-removal search. Each level-3+ case sets prefillCandidates = 
  true; TestViewModel then calls a new GameReducer.populateValidCandidates
  before the hint so the technique actually has notes to operate on. Ordering
  newest-first: XY-Wing → X-Wing → Hidden Triple → Naked Triple → Box-Line →
  Pointing Pair → Hidden Pair → Naked Pair → Candidate Sweep → Hidden Single →
  Naked Single.

  Known gap: the random search couldn't surface a Swordfish against the single
  canonical solution within the time budget. Swordfish is genuinely uncommon —
  when you encounter a real puzzle that needs it, the engine and animation are
  ready, but the Test menu doesn't preview it yet. Marked with a comment in
  HintTestCases.

❯ Hint is looping on a single naked pair. It doesn't recognize that finding a   
  naked pair that doesn't have any canidates to remove isn't useful             

● Good catch. The engine computes candidates from grid values only, so once a
  pair eliminates a digit from a peer's player marks, the engine's view of
  candidates doesn't change — it keeps re-finding the same pair with the same
  plan.

  Fix: when player marks are available, treat them (filtered to "still legal"
  digits) as the cell's candidate set. After an elimination applies, the next
  find() sees the updated marks and the plan for that pair is empty, so the
  engine moves on.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 1 line, removed 1 line
      205          findNakedSingle(grid)?.let { return it }
      206          findHiddenSingle(grid)?.let { return it }
      207          findCandidateSweepInternal(grid, marks)?.let { return 
           it }
      208 -        val cands = candidateGrid(grid)                       
      208 +        val cands = candidateGrid(grid, marks)                
      209          findNakedPair(grid, cands)?.let { return it }
      210          findHiddenPair(grid, cands)?.let { return it }
      211          findPointingPair(grid, cands)?.let { return it }

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 22 lines, removed 10 lines
      337      // ---- Level 3+ candidate-based techniques ----
      338  
      339      /**
      340 -     * Per-cell candidate set computed solely from grid values
          -: for each                                                    
      341 -     * empty cell, the digits not yet placed in its row/column
          -/box. Used                                                    
      342 -     * by the pair/triple/wing finders so deductions don't dep
          -end on the                                                    
      343 -     * player's possibly-incomplete pencil marks.             
      340 +     * Per-cell candidate set used by pair/triple/wing finders
          +. When the                                                    
      341 +     * player has pencil marks for a cell, those marks (filter
          +ed to the                                                     
      342 +     * digits still legal given grid values) are the effective
          + candidates                                                   
      343 +     * — so once a higher-level technique has erased a digit f
          +rom a peer's                                                  
      344 +     * marks, the next [find] call no longer treats that digit
          + as a                                                         
      345 +     * candidate for that cell and the same hint can't be retu
          +rned again.                                                   
      346 +     *                                                        
      347 +     * When a cell has no player marks, fall back to the full 
          +set of                                                        
      348 +     * digits that grid values don't yet rule out.            
      349       */
      345 -    private fun candidateGrid(grid: IntArray): Array<Set<Int>>
          - = Array(81) { idx ->                                         
      346 -        if (grid[idx] != 0) {                                 
      347 -            emptySet()                                        
      348 -        } else {                                              
      350 +    private fun candidateGrid(grid: IntArray, marks: Array<Set
          +<Int>>?): Array<Set<Int>> =                                   
      351 +        Array(81) { idx ->                                    
      352 +            if (grid[idx] != 0) return@Array emptySet()       
      353              val r = idx / 9; val c = idx % 9
      350 -            (1..9).filter { SudokuSolver.isValidPlacement(grid
          -, r, c, it) }.toSet()                                         
      354 +            val playerMarks = marks?.get(idx) ?: emptySet()   
      355 +            if (playerMarks.isNotEmpty()) {                   
      356 +                playerMarks.filterTo(mutableSetOf()) {        
      357 +                    SudokuSolver.isValidPlacement(grid, r, c, 
          +it)                                                           
      358 +                }                                             
      359 +            } else {                                          
      360 +                (1..9).filterTo(mutableSetOf()) {             
      361 +                    SudokuSolver.isValidPlacement(grid, r, c, 
          +it)                                                           
      362 +                }                                             
      363 +            }                                                 
      364          }
      352 -    }                                                         
      365  
      366      private fun findNakedPair(grid: IntArray, cands: Array<Set
           <Int>>): NakedPair? {
      367          for (unit in UNITS_ORDERED) {

● Add a test that locks in the new behavior — applying a naked pair must keep
  the engine from re-emitting it:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 33 lines
      187      }
      188  
      189      @Test
      190 +    fun `Naked Pair stops firing after its eliminations are ap
          +plied`() {                                                    
      191 +        // The Naked Pair test-menu grid (same one the search 
          +tool found).                                                  
      192 +        val givens = intArrayOf(                              
      193 +            7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 46, 
          +47, 52, 53,                                                   
      194 +            54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74, 75
          +, 78, 79,                                                     
      195 +        )                                                     
      196 +        val grid = IntArray(81)                               
      197 +        for (idx in givens) grid[idx] = CANONICAL_SOLUTION[idx
          +]                                                             
      198 +        // Pretend candidates were just filled in (valid candi
          +dates per cell).                                              
      199 +        val marks = Array(81) { idx ->                        
      200 +            if (grid[idx] != 0) emptySet() else (1..9).filter 
          +{                                                             
      201 +                SudokuSolver.isValidPlacement(grid, idx / 9, i
          +dx % 9, it)                                                   
      202 +            }.toMutableSet() as MutableSet<Int>               
      203 +        }                                                     
      204 +                                                              
      205 +        val first = HintEngine.find(grid, marks)              
      206 +        assertTrue(first is HintEngine.NakedPair, "expected Na
          +kedPair, got $first")                                         
      207 +        first as HintEngine.NakedPair                         
      208 +                                                              
      209 +        // Apply the pair's eliminations to the player marks. 
      210 +        for (step in first.plan) {                            
      211 +            (marks[step.cellIdx] as MutableSet).removeAll(step
          +.doomedDigits)                                                
      212 +        }                                                     
      213 +                                                              
      214 +        // The same Naked Pair must not be returned again.    
      215 +        val second = HintEngine.find(grid, marks)             
      216 +        if (second is HintEngine.NakedPair) {                 
      217 +            assertTrue(second.keyCells != first.keyCells,     
      218 +                "engine should not re-emit the same Naked Pair
          + after its plan is applied")                                  
      219 +        }                                                     
      220 +    }                                                         
      221 +                                                              
      222 +    @Test                                                     
      223      fun `returns null on a fully solved grid`() {
      224          assertNull(HintEngine.find(CANONICAL_SOLUTION))
      225      }

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● All tests green. Commit and push:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Fix hint engine looping on a single Naked Pair after its eliminations
      apply…)
  ⎿  [claude/add-sudoku-hint-button-L1spT f7f55b8] Fix hint engine looping
     on a single Naked Pair after its eliminations apply
      2 files changed, 56 insertions(+), 11 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed fe5be73.

  Root cause: candidateGrid returned valid candidates computed purely from grid
  values. Once a Naked Pair (or any level-3+ technique) erased a digit from a
  peer's pencil marks, the engine still saw the digit as a candidate of that
  cell, so the same pair kept being a viable hint forever.

  Fix: candidateGrid(grid, marks) now uses player marks (filtered to "still
  legal" digits) when a cell has any marks, falling back to the full valid set
  only for empty-mark cells. After the pair's plan is applied, the peer marks no
  longer contain the doomed digits, so findNakedPair builds an empty plan and
  the engine moves on to the next finder. New test asserts the same Naked Pair
  can't come back after its eliminations are applied.

❯ I also have a strategy that I do not know the name for. Here is an example of 
  it                                                                            
  Column 4 has only two unsolved cells both with 4 as a canidate. C4R5 and C4R9 
  All the candidates for 4 in Square 4 (Middle Left) on in Row 5 or Column 1    
  We can eliminate 4 as a candidate in C1R9 because a 4 in C1R9 would make C4R5 
  a 4 and eliminate all canidates for 4 in Square 4                             
                                                                                
  We also need to create some tests.                                            
  Can you write out some .md files that describe in plain english how the tests 
  should be run, then use those md files to write tests for this program?       
  Something I can run locally without porting an apk to my phone or running an  
  emulator?                                                                     

● That technique is called an Empty Rectangle (ER): inside a box, a digit's
  candidates form an L-shape — all in one row plus all in one column — and a
  conjugate pair elsewhere has one endpoint on the row leg, letting you
  eliminate the digit at the intersection of the conjugate's other endpoint and
  the column leg (or vice versa). I'll implement it, then write plain-English
  test docs and the JUnit tests they describe.

  Adding Empty Rectangle to the engine:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 17 lines
      173          override val techniqueName: String = "X-Wing"
      174      }
      175  
      176 +    /**                                                       
      177 +     * Inside a box, a digit's candidates all lie on a single 
          +row + a                                                       
      178 +     * single column (an L shape). A conjugate pair elsewhere 
          +supplies a                                                    
      179 +     * "strong link" with one endpoint on the row leg outside 
          +the box —                                                     
      180 +     * the elimination is the cell at (free-end-row, column-le
          +g).                                                           
      181 +     *                                                        
      182 +     * Symmetric for a conjugate pair on the row through the c
          +ol leg.                                                       
      183 +     */                                                       
      184 +    data class EmptyRectangle(                                
      185 +        override val keyCells: Set<Int>,                      
      186 +        override val unitCells: Set<Int>,                     
      187 +        override val plan: List<EliminationStep>,             
      188 +        val digit: Int,                                       
      189 +    ) : EliminationHint {                                     
      190 +        override val techniqueName: String = "Empty Rectangle"
      191 +    }                                                         
      192 +                                                              
      193      data class Swordfish(
      194          override val keyCells: Set<Int>,
      195          override val unitCells: Set<Int>,

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 1 line
      230          findNakedTriple(grid, cands)?.let { return it }
      231          findHiddenTriple(grid, cands)?.let { return it }
      232          findXWing(grid, cands)?.let { return it }
      233 +        findEmptyRectangle(grid, cands)?.let { return it }    
      234          findSwordfish(grid, cands)?.let { return it }
      235          findXYWing(grid, cands)?.let { return it }
      236          return null

● Insert the finder. Putting it next to findXWing:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 84 lines
      684          return null
      685      }
      686  
      687 +    private fun findEmptyRectangle(grid: IntArray, cands: Arra
          +y<Set<Int>>): EmptyRectangle? {                               
      688 +        for (d in 1..9) {                                     
      689 +            for (boxIdx in 0 until 9) {                       
      690 +                val boxRow0 = (boxIdx / 3) * 3                
      691 +                val boxCol0 = (boxIdx % 3) * 3                
      692 +                val boxRows = boxRow0..boxRow0 + 2            
      693 +                val boxCols = boxCol0..boxCol0 + 2            
      694 +                val boxCells = buildList {                    
      695 +                    for (r in boxRows) for (c in boxCols) add(
          +r * 9 + c)                                                    
      696 +                }                                             
      697 +                val cellsWithD = boxCells.filter { grid[it] ==
          + 0 && d in cands[it] }                                        
      698 +                if (cellsWithD.size < 2) continue             
      699 +                                                              
      700 +                // Find a row R and column C inside the box su
          +ch that every                                                 
      701 +                // candidate cell is either on R or on C, with
          + at least one                                                 
      702 +                // candidate strictly in the row leg AND at le
          +ast one strictly                                              
      703 +                // in the column leg (i.e. not just a pointing
          + pair).                                                       
      704 +                for (R in boxRows) {                          
      705 +                    for (C in boxCols) {                      
      706 +                        val outsideRC = cellsWithD.filter { it
          + / 9 != R && it % 9 != C }                                    
      707 +                        if (outsideRC.isNotEmpty()) continue  
      708 +                        val inRow = cellsWithD.filter { it / 9
          + == R && it % 9 != C }                                        
      709 +                        val inCol = cellsWithD.filter { it / 9
          + != R && it % 9 == C }                                        
      710 +                        if (inRow.isEmpty() || inCol.isEmpty()
          +) continue                                                    
      711 +                                                              
      712 +                        // Type A: strong link is a column c o
          +utside the box.                                               
      713 +                        // Matching endpoint sits on row R, ot
          +her endpoint is                                               
      714 +                        // outside the box -> eliminate d at (
          +otherEndRow, C).                                              
      715 +                        for (c in 0..8) {                     
      716 +                            if (c in boxCols) continue        
      717 +                            val colCells = (0..8).filter { r -
          +>                                                             
      718 +                                grid[r * 9 + c] == 0 && d in c
          +ands[r * 9 + c]                                               
      719 +                            }                                 
      720 +                            if (colCells.size != 2) continue  
      721 +                            if (R !in colCells) continue      
      722 +                            val freeRow = (colCells - R).first
          +()                                                            
      723 +                            if (freeRow in boxRows) continue  
      724 +                            val elim = freeRow * 9 + C        
      725 +                            if (grid[elim] != 0 || d !in cands
          +[elim]) continue                                              
      726 +                                                              
      727 +                            val conj = setOf(R * 9 + c, freeRo
          +w * 9 + c)                                                    
      728 +                            return EmptyRectangle(            
      729 +                                keyCells = cellsWithD.toSet() 
          ++ conj,                                                       
      730 +                                unitCells = boxCells.toSet() +
          + (0..8).map { it * 9 + c }.toSet(),                           
      731 +                                plan = listOf(EliminationStep(
      732 +                                    cellIdx = elim,           
      733 +                                    doomedDigits = setOf(d),  
      734 +                                    explainerCells = cellsWith
          +D.toSet() + conj,                                             
      735 +                                )),                           
      736 +                                digit = d,                    
      737 +                            )                                 
      738 +                        }                                     
      739 +                        // Type B: strong link is a row r outs
          +ide the box.                                                  
      740 +                        for (r in 0..8) {                     
      741 +                            if (r in boxRows) continue        
      742 +                            val rowCells = (0..8).filter { c -
          +>                                                             
      743 +                                grid[r * 9 + c] == 0 && d in c
          +ands[r * 9 + c]                                               
      744 +                            }                                 
      745 +                            if (rowCells.size != 2) continue  
      746 +                            if (C !in rowCells) continue      
      747 +                            val freeCol = (rowCells - C).first
          +()                                                            
      748 +                            if (freeCol in boxCols) continue  
      749 +                            val elim = R * 9 + freeCol        
      750 +                            if (grid[elim] != 0 || d !in cands
          +[elim]) continue                                              
      751 +                                                              
      752 +                            val conj = setOf(r * 9 + C, r * 9 
          ++ freeCol)                                                    
      753 +                            return EmptyRectangle(            
      754 +                                keyCells = cellsWithD.toSet() 
          ++ conj,                                                       
      755 +                                unitCells = boxCells.toSet() +
          + (0..8).map { r * 9 + it }.toSet(),                           
      756 +                                plan = listOf(EliminationStep(
      757 +                                    cellIdx = elim,           
      758 +                                    doomedDigits = setOf(d),  
      759 +                                    explainerCells = cellsWith
          +D.toSet() + conj,                                             
      760 +                                )),                           
      761 +                                digit = d,                    
      762 +                            )                                 
      763 +                        }                                     
      764 +                    }                                         
      765 +                }                                             
      766 +            }                                                 
      767 +        }                                                     
      768 +        return null                                           
      769 +    }                                                         
      770 +                                                              
      771      private fun findSwordfish(grid: IntArray, cands: Array<Set
           <Int>>): Swordfish? {
      772          for (d in 1..9) {
      773              val rowPos = Array(9) { r ->

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● Let me commit the engine work, then write the markdown test plan and tests:
● Bash(git add -A && git commit -m "Add Empty Rectangle technique to hint
      engine" && git push 2>&1 | tail -3)
  ⎿  [claude/add-sudoku-hint-button-L1spT 1aad1d1] Add Empty Rectangle tech
     nique to hint engine
      1 file changed, 102 insertions(+)
     … +4 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -2 &&
      git rebase origin/claude/add-sudoku-hint-button-L1spT 2>&1 | tail -3 &&
      git push 2>&1 | t…)
  ⎿   * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        fe5be73..c75e07d  claude/add-sudoku-hint-button-L1spT -> origin/cla
     ude/add-sudoku-hint-button-L1spT
     … +5 lines (ctrl+o to expand)

● Now write the markdown test plan docs:

● Write(/home/user/soduku-trainer/docs/testing/README.md)
  ⎿  Wrote 54 lines to ../../../user/soduku-trainer/docs/testing/README.md
       2 # Testing
       3 This project's logic lives in the pure-JVM `:domain` module, so 
         the
       4 full test suite runs **without an emulator, an APK, or a device*
         *.
       6 ## Running the tests
       7 
       8 From the repository root:
       9 
      10 ```bash
     … +51 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/docs/testing/hint-engine-tests.md)
  ⎿  Wrote 76 lines to
     ../../../user/soduku-trainer/docs/testing/hint-engine-tests.md
       2 # Hint Engine Tests
       3 `HintEngine.find(grid, marks?)` walks the technique cascade in t
         his
       4 order and returns the first technique that has work to do:
       5 
       6 ```
       7 Naked Single
       8   -> Hidden Single
       9     -> Candidate Sweep
      10       -> Naked Pair  -> Hidden Pair
     … +74 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/docs/testing/reducer-tests.md)
  ⎿  Wrote 69 lines to
     ../../../user/soduku-trainer/docs/testing/reducer-tests.md
       2 # Reducer Tests
       3 `GameReducer` is a pure state machine. Every test starts from
       4 `GameState.forNewGame(puzzle)`, applies a sequence of reducer ca
         lls,
       5 and asserts on the resulting `GameState`. No coroutines, no Andr
       6 no animator.
       7 
       8 ## Digit selection
       9 
      10 - **`selectDigit sets active and re-selecting deactivates`** —
     … +60 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/docs/testing/undo-tests.md)
  ⎿  Wrote 55 lines to
     ../../../user/soduku-trainer/docs/testing/undo-tests.md
       2 # Undo Tests
       3 Undo must reverse exactly one move at a time, restoring everythi
         ng
       4 the move did. Each `Move` subclass has its own undo behavior; th
         is
       6 doc lists what each subclass must restore.
       7 ## Move.SetValue
       8 
       9 - Restore the cell's previous (empty) state.
      10 - Restore the cell's previous pencil marks via `clearedMarks`.
     … +47 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/docs/testing/progressive-hint-tests.md)
  ⎿  Wrote 51 lines to
     ../../../user/soduku-trainer/docs/testing/progressive-hint-tests.md
      2 # Progressive Hint Tests
      3 The Hint button stages a deduction across three presses:
      4 
      5 1. **Press 1**: highlight the pattern's key cell(s). Start a 60-s
        econd
      6    timer (config: `pending_hint_ms`).
      7 2. **Press 2**: also reveal the technique name. Restart the 60-se
        cond
      8    timer.
      9 3. **Press 3**: clear the pending state and fire the animation.
     … +44 lines (ctrl+o to expand)

● Write(/home/user/soduku-trainer/docs/testing/sweep-tests.md)
  ⎿  Wrote 45 lines to
     ../../../user/soduku-trainer/docs/testing/sweep-tests.md
      1 # Candidate Sweep Tests
      2 
      3 Candidate Sweep is both a hint technique and a standalone player
      4 action (the Sweep button). Both paths share the same reducer
      5 machinery but differ in whether `hintCount` increments.
      6 
      7 ## Engine behavior
      8 
      9 In `HintEngineTest.kt`:
     … +38 lines (ctrl+o to expand)

● Let me expose findEmptyRectangle and candidateGrid as internal so the test can
  target ER directly without grid-engineering it through the whole cascade,
  then add the missing tests.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 5 lines, removed 1 line
      364       *
      365       * When a cell has no player marks, fall back to the full 
           set of
      366       * digits that grid values don't yet rule out.
      367 +     *                                                        
      368 +     * Marked `internal` for test isolation — tests that exerc
          +ise a                                                         
      369 +     * single finder directly need to construct the same candi
          +date set                                                      
      370 +     * the engine would compute.                              
      371       */
      368 -    private fun candidateGrid(grid: IntArray, marks: Array<Set
          -<Int>>?): Array<Set<Int>> =                                   
      372 +    internal fun candidateGrid(grid: IntArray, marks: Array<Se
          +t<Int>>?): Array<Set<Int>> =                                  
      373          Array(81) { idx ->
      374              if (grid[idx] != 0) return@Array emptySet()
      375              val r = idx / 9; val c = idx % 9

  Searched for 1 pattern (ctrl+o to expand)

● Bash(sed -i 's/private fun findNakedPair/internal fun findNakedPair/;
      s/private fun findHiddenPair/internal fun findHiddenPair/; s/private fun
      findPointingPair/inter…)
  ⎿      internal fun findNakedPair(grid: IntArray, cands: Array<Set<Int>>)
     : NakedPair? {
         internal fun findHiddenPair(grid: IntArray, cands: Array<Set<Int>>
     … +16 lines (ctrl+o to expand)

● Now write the missing tests in HintEngineTest.kt — the per-technique tests +
  Empty Rectangle:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 140 lines
      219          }
      220      }
      221  
      222 +    // ---- Level 3+ technique grids ----                     
      223 +                                                              
      224 +    /**                                                       
      225 +     * Build the test grid + valid-candidate marks for the ind
          +ices the                                                      
      226 +     * Test-menu uses for [techniqueName], then assert [HintEn
          +gine.find]                                                    
      227 +     * classifies the next deduction as [expectedClass].      
      228 +     */                                                       
      229 +    private fun assertTechniqueGridReturns(                   
      230 +        techniqueName: String,                                
      231 +        givenIndices: IntArray,                               
      232 +        check: (HintEngine.Hint) -> Boolean,                  
      233 +    ) {                                                       
      234 +        val grid = IntArray(81)                               
      235 +        for (idx in givenIndices) grid[idx] = CANONICAL_SOLUTI
          +ON[idx]                                                       
      236 +        val marks = Array(81) { idx ->                        
      237 +            if (grid[idx] != 0) emptySet() else (1..9).filter 
          +{                                                             
      238 +                SudokuSolver.isValidPlacement(grid, idx / 9, i
          +dx % 9, it)                                                   
      239 +            }.toSet()                                         
      240 +        }                                                     
      241 +        val hint = HintEngine.find(grid, marks)               
      242 +        assertNotNull(hint, "$techniqueName: find() returned n
          +ull")                                                         
      243 +        assertTrue(check(hint!!), "$techniqueName: got ${hint:
          +:class.simpleName}")                                          
      244 +    }                                                         
      245 +                                                              
      246      @Test
      247 +    fun `Naked Pair on the test-menu grid`() {                
      248 +        assertTechniqueGridReturns(                           
      249 +            "Naked Pair",                                     
      250 +            intArrayOf(                                       
      251 +                7, 8, 12, 16, 18, 22, 24, 28, 29, 31, 39, 42, 
          +46, 47, 52, 53,                                               
      252 +                54, 56, 58, 60, 62, 63, 65, 66, 67, 70, 71, 74
          +, 75, 78, 79,                                                 
      253 +            ),                                                
      254 +        ) { it is HintEngine.NakedPair }                      
      255 +    }                                                         
      256 +                                                              
      257 +    @Test                                                     
      258 +    fun `Hidden Pair on the test-menu grid`() {               
      259 +        assertTechniqueGridReturns(                           
      260 +            "Hidden Pair",                                    
      261 +            intArrayOf(                                       
      262 +                8, 12, 16, 19, 20, 21, 23, 24, 25, 30, 33, 40,
          + 45, 48, 50,                                                  
      263 +                53, 55, 56, 61, 63, 67, 69, 71, 74, 77, 78,   
      264 +            ),                                                
      265 +        ) { it is HintEngine.HiddenPair }                     
      266 +    }                                                         
      267 +                                                              
      268 +    @Test                                                     
      269 +    fun `Pointing Pair on the test-menu grid`() {             
      270 +        assertTechniqueGridReturns(                           
      271 +            "Pointing Pair",                                  
      272 +            intArrayOf(                                       
      273 +                3, 4, 6, 10, 14, 24, 29, 33, 36, 37, 38, 43, 4
          +4, 45, 47, 52,                                                
      274 +                53, 56, 65, 66, 67, 68, 69, 71, 78, 80,       
      275 +            ),                                                
      276 +        ) { it is HintEngine.PointingPair }                   
      277 +    }                                                         
      278 +                                                              
      279 +    @Test                                                     
      280 +    fun `Box-Line Reduction on the test-menu grid`() {        
      281 +        assertTechniqueGridReturns(                           
      282 +            "Box-Line Reduction",                             
      283 +            intArrayOf(                                       
      284 +                18, 19, 20, 22, 24, 27, 32, 33, 34, 35, 37, 42
          +, 52, 53, 57,                                                 
      285 +                62, 65, 69, 70, 72, 77,                       
      286 +            ),                                                
      287 +        ) { it is HintEngine.BoxLineReduction }               
      288 +    }                                                         
      289 +                                                              
      290 +    @Test                                                     
      291 +    fun `Naked Triple on the test-menu grid`() {              
      292 +        assertTechniqueGridReturns(                           
      293 +            "Naked Triple",                                   
      294 +            intArrayOf(                                       
      295 +                0, 2, 10, 13, 16, 17, 19, 22, 28, 32, 33, 41, 
          +44, 47, 48, 49,                                               
      296 +                54, 62, 63, 74, 78,                           
      297 +            ),                                                
      298 +        ) { it is HintEngine.NakedTriple }                    
      299 +    }                                                         
      300 +                                                              
      301 +    @Test                                                     
      302 +    fun `Hidden Triple on the test-menu grid`() {             
      303 +        assertTechniqueGridReturns(                           
      304 +            "Hidden Triple",                                  
      305 +            intArrayOf(                                       
      306 +                6, 7, 13, 23, 37, 40, 42, 43, 51, 65, 69, 70, 
          +71, 72, 74, 79,                                               
      307 +            ),                                                
      308 +        ) { it is HintEngine.HiddenTriple }                   
      309 +    }                                                         
      310 +                                                              
      311 +    @Test                                                     
      312 +    fun `X-Wing on the test-menu grid`() {                    
      313 +        assertTechniqueGridReturns(                           
      314 +            "X-Wing",                                         
      315 +            intArrayOf(                                       
      316 +                0, 1, 10, 12, 14, 16, 20, 21, 26, 28, 32, 46, 
          +49, 51, 64, 65,                                               
      317 +                68, 70, 75, 77, 79,                           
      318 +            ),                                                
      319 +        ) { it is HintEngine.XWing }                          
      320 +    }                                                         
      321 +                                                              
      322 +    @Test                                                     
      323 +    fun `XY-Wing on the test-menu grid`() {                   
      324 +        assertTechniqueGridReturns(                           
      325 +            "XY-Wing",                                        
      326 +            intArrayOf(                                       
      327 +                2, 5, 6, 10, 11, 15, 16, 24, 28, 35, 37, 39, 4
          +2, 44, 52, 55,                                                
      328 +                57, 60, 62, 67, 70, 71, 73, 74, 76, 78, 79,   
      329 +            ),                                                
      330 +        ) { it is HintEngine.XYWing }                         
      331 +    }                                                         
      332 +                                                              
      333 +    @Test                                                     
      334 +    fun `Empty Rectangle eliminates the corner cell`() {      
      335 +        // The worked example from docs/testing/hint-engine-te
          +sts.md:                                                       
      336 +        //  - box 3 (rows 3-5, cols 0-2) has all 4-candidates 
          +on row 4 or col 0                                             
      337 +        //  - col 3 has exactly two 4-candidates at (4,3) and 
          +(8,3)                                                         
      338 +        //  - elimination: 4 at (8,0)                         
      339 +        // Built with a fill value of 1 so peer-blocking doesn
          +'t affect digit 4,                                            
      340 +        // and explicit marks of {4} on every cell that should
          + hold the candidate.                                          
      341 +        val grid = IntArray(81) { 1 }                         
      342 +        val empties = listOf(                                 
      343 +            3 * 9 + 0, 4 * 9 + 0, 4 * 9 + 1, 4 * 9 + 2, 5 * 9 
          ++ 0,                                                          
      344 +            4 * 9 + 3, 8 * 9 + 3, 8 * 9 + 0,                  
      345 +        )                                                     
      346 +        for (idx in empties) grid[idx] = 0                    
      347 +        val marks = Array<Set<Int>>(81) { emptySet() }        
      348 +        for (idx in empties) marks[idx] = setOf(4)            
      349 +                                                              
      350 +        val cands = HintEngine.candidateGrid(grid, marks)     
      351 +        val hint = HintEngine.findEmptyRectangle(grid, cands) 
      352 +                                                              
      353 +        assertNotNull(hint, "ER finder returned null on the wo
          +rked example")                                                
      354 +        hint!!                                                
      355 +        assertEquals(4, hint.digit)                           
      356 +        assertEquals(1, hint.plan.size)                       
      357 +        val step = hint.plan[0]                               
      358 +        assertEquals(8 * 9 + 0, step.cellIdx, "elimination mus
          +t target (8,0)")                                              
      359 +        assertTrue(4 in step.doomedDigits)                    
      360 +    }                                                         
      361 +                                                              
      362 +    @Test                                                     
      363      fun `returns null on a fully solved grid`() {
      364          assertNull(HintEngine.find(CANONICAL_SOLUTION))
      365      }

● Add the multi-cell pending hint test:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 27 lines
      690      }
      691  
      692      @Test
      693 +    fun `pending hint covers multi-cell patterns`() {         
      694 +        // Use a fake hint with multiple keyCells so we exerci
          +se the                                                        
      695 +        // "any cell in keyCells clears it" branch of tapCell.
      696 +        val s0 = initial()                                    
      697 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      698 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      699 +        // Pick three non-given empty cells as a multi-cell pa
          +ttern.                                                        
      700 +        val emptyIndices = (0 until 81).filter { !s0.cells[it]
          +.given && s0.cells[it].value == null }                        
      701 +        val keys = emptyIndices.take(3).toSet()               
      702 +        check(keys.size == 3)                                 
      703 +        val otherEmpty = emptyIndices.first { it !in keys }   
      704 +                                                              
      705 +        // Tap each key in turn -> hint clears                
      706 +        for (key in keys) {                                   
      707 +            var s = GameReducer.setPendingHint(s0, hint, keys)
      708 +            s = GameReducer.selectDigit(s, 1)                 
      709 +            s = GameReducer.tapCell(s, key / 9, key % 9)      
      710 +            assertNull(s.pendingHint, "tapping key cell $key s
          +hould clear the pending hint")                                
      711 +        }                                                     
      712 +        // Tapping a non-key cell preserves it.               
      713 +        var s = GameReducer.setPendingHint(s0, hint, keys)    
      714 +        s = GameReducer.selectDigit(s, 1)                     
      715 +        s = GameReducer.tapCell(s, otherEmpty / 9, otherEmpty 
          +% 9)                                                          
      716 +        assertNotNull(s.pendingHint, "tapping a non-key cell m
          +ust not clear the pending hint")                              
      717 +    }                                                         
      718 +                                                              
      719 +    @Test                                                     
      720      fun `placing a value in a different cell does not clear a 
           pending hint`() {
      721          val s0 = initial()
      722          val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
           }

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -20)
  ⎿  To honour the JVM settings for this build a single-use Daemon process
     will be forked. For more on this, please refer to https://docs.gradle.
     org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in th
     … +16 lines (ctrl+o to expand)

● All tests pass — 11 new ones covering Empty Rectangle, all 8 level-3+
  techniques on the Test-menu grids, and the multi-cell PendingHint flow. Commit
  and push:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Empty Rectangle technique and plain-English test docs + tests…)
  ⎿  [claude/add-sudoku-hint-button-L1spT efc29ab] Add Empty Rectangle tech
     nique and plain-English test docs + tests
      9 files changed, 532 insertions(+), 11 deletions(-)
     … +11 lines (ctrl+o to expand)

● Pushed efc29ab.

  Empty Rectangle technique is now in the engine cascade between X-Wing and
  Swordfish, with the user's worked example translated into the test.

  Test docs under docs/testing/:
  - README.md — how to run tests locally, what each file covers, why no emulator
  is needed
  - hint-engine-tests.md — the cascade and each technique's test
  - reducer-tests.md — board / taps / pencil / undo
  - undo-tests.md — per-Move-subclass undo expectations
  - progressive-hint-tests.md — the three-stage Hint button flow
  - sweep-tests.md — Candidate Sweep through hint vs Sweep-button

  New tests (all in :domain, runs on plain JVM via ./gradlew :domain:test 
  -PincludeApp=false):
  - Empty Rectangle eliminates the corner cell — calls findEmptyRectangle
  directly on the user's worked example.
  - One test per level-3+ technique (Naked Pair, Hidden Pair, Pointing Pair,
  Box-Line Reduction, Naked Triple, Hidden Triple, X-Wing, XY-Wing) — each
  asserts HintEngine.find returns the right subclass on the same grid the Test
  menu shows.
  - pending hint covers multi-cell patterns — placing a value in any keyCell
  clears the pending hint; placing in a non-key cell preserves it.

  Level-3+ finders are now internal (still hidden from the :app module's normal
  callers, but reachable from the same-module tests) so each test can target one
  technique without engineering a whole-cascade grid.

❯ We need a 'show again' button that is only enabled when a hint is shown.      
  Pushing it will undo the hint, then reapply it but with a much slower         
  animation                                                                     

● Plan: track lastHint: HintEngine.Hint? on GameState. Set when an animation
  completes; clear on any board-changing player action (tap, double-tap, reset)
  — but not on undo, so undo+Show Again works. The button calls onShowAgain()
  which undoes the hint's move and re-launches the animator with a slower speed
  and countAsHint = false.
● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameState.kt)
  ⎿  Added 1 line
      11      val hintCount: Int = 0,
      12      val hintUi: HintUi? = null,
      13      val pendingHint: PendingHint? = null,
      14 +    val lastHint: HintEngine.Hint? = null,                     
      15  ) {
      16      enum class Phase { Playing, AutoSolving, Celebrating, Hinti
          ng }
      17  

● Now thread lastHint through the completion reducers and add a countAsHint flag
  to all of them:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 7 lines, removed 2 lines
      280       * Records a [Move.SetValue] so undo reverses both the pla
           cement and
      281       * any pencil marks the animation overwrote in the target 
           cell.
      282       */
      283 -    fun completeNakedSingleHint(state: GameState, hint: HintEn
          -gine.NakedSingle): GameState {                                
      283 +    fun completeNakedSingleHint(                              
      284 +        state: GameState,                                     
      285 +        hint: HintEngine.NakedSingle,                         
      286 +        countAsHint: Boolean = true,                          
      287 +    ): GameState {                                            
      288          val ui = state.hintUi as? HintUi.NakedSingle
      289          val target = state.cells[hint.targetCell]
      290          val placed = target.copy(
     ...
      304              .copy(
      305                  phase = GameState.Phase.Playing,
      306                  hintUi = null,
      303 -                hintCount = state.hintCount + 1,              
      307 +                hintCount = if (countAsHint) state.hintCount +
          + 1 else state.hintCount,                                      
      308                  history = state.history + move,
      309 +                lastHint = hint,                              
      310              )
      311      }
      312  

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 7 lines, removed 2 lines
      384       * Records a [Move.SetValue] whose autoClearedMarks restor
           e any
      385       * player-placed solution-digit notes that the sweep erase
           d.
      386       */
      387 -    fun completeHiddenSingleHint(state: GameState, hint: HintE
          -ngine.HiddenSingle): GameState {                              
      387 +    fun completeHiddenSingleHint(                             
      388 +        state: GameState,                                     
      389 +        hint: HintEngine.HiddenSingle,                        
      390 +        countAsHint: Boolean = true,                          
      391 +    ): GameState {                                            
      392          val ui = state.hintUi as? HintUi.HiddenSingle
      393          val target = state.cells[hint.targetCell]
      394          var s = state.replaceCell(target.copy(
     ...
      414          return s.copy(
      415              phase = GameState.Phase.Playing,
      416              hintUi = null,
      413 -            hintCount = state.hintCount + 1,                  
      417 +            hintCount = if (countAsHint) state.hintCount + 1 e
          +lse state.hintCount,                                          
      418              history = state.history + move,
      419 +            lastHint = hint,                                  
      420          )
      421      }
      422  

● Now the Candidate Sweep and Elimination completions — they already have
  countAsHint; just need to set lastHint:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 1 line
      526              hintUi = null,
      527              hintCount = if (countAsHint) state.hintCount + 1 e
           lse state.hintCount,
      528              history = state.history + move,
      529 +            lastHint = hint,                                  
      530          )
      531      }
      532  

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 7 lines, removed 2 lines
      576          ))
      577      }
      578  
      579 -    fun completeEliminationHint(state: GameState, hint: HintEn
          -gine.EliminationHint): GameState {                            
      579 +    fun completeEliminationHint(                              
      580 +        state: GameState,                                     
      581 +        hint: HintEngine.EliminationHint,                     
      582 +        countAsHint: Boolean = true,                          
      583 +    ): GameState {                                            
      584          val ui = state.hintUi as? HintUi.Elimination
      585          val prior = ui?.priorMarks.orEmpty()
      586          val current = buildList {
     ...
      599          return state.copy(
      600              phase = GameState.Phase.Playing,
      601              hintUi = null,
      598 -            hintCount = state.hintCount + 1,                  
      602 +            hintCount = if (countAsHint) state.hintCount + 1 e
          +lse state.hintCount,                                          
      603              history = state.history + move,
      604 +            lastHint = hint,                                  
      605          )
      606      }
      607  

● Clear lastHint on tap / double-tap that actually change cells:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 6 lines, removed 4 lines
      72              valueColor = CellColor.Guess(state.activeColor),
      73              pencilMarks = emptyMap(),
      74          )
      75 -        // Placing a value in any of the pending-hint key cells
         - cancels it.                                                   
      75 +        // Placing a value in any of the pending-hint key cells
         + cancels it,                                                   
      76 +        // and any board-changing player action invalidates "Sh
         +ow Again".                                                     
      77          val idx = cell.row * 9 + cell.col
      77 -        val cleared =                                          
      78 -            if (s.pendingHint?.keyCells?.contains(idx) == true)
         - s.copy(pendingHint = null)                                    
      79 -            else s                                             
      78 +        var cleared = s.copy(lastHint = null)                  
      79 +        if (cleared.pendingHint?.keyCells?.contains(idx) == tru
         +e) {                                                           
      80 +            cleared = cleared.copy(pendingHint = null)         
      81 +        }                                                      
      82          return cleared.replaceCell(newCell).appendHistory(move)
      83      }
      84  

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 4 lines, removed 1 line
      107              )
      108              m to move
      109          }
      110 -        return state.replaceCell(cell.copy(pencilMarks = newMa
          -rks)).appendHistory(move)                                     
      110 +        return state                                          
      111 +            .copy(lastHint = null)                            
      112 +            .replaceCell(cell.copy(pencilMarks = newMarks))   
      113 +            .appendHistory(move)                              
      114      }
      115  
      116      fun doubleTapCell(state: GameState, row: Int, col: Int): G
           ameState {

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 4 lines, removed 1 line
      128              prevValueColor = prevColor.color,
      129          )
      130          val newCell = cell.copy(value = null, valueColor = nul
           l)
      131 -        return state.replaceCell(newCell).appendHistory(move) 
      131 +        return state                                          
      132 +            .copy(lastHint = null)                            
      133 +            .replaceCell(newCell)                             
      134 +            .appendHistory(move)                              
      135      }
      136  
      137      fun undo(state: GameState): GameState {

● Now add speedMultiplier to HintAnimator:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 20 lines, removed 5 lines
      26  
      27      @Volatile private var pending: HintEngine.Hint? = null
      28      @Volatile private var pendingCountAsHint: Boolean = true
      29 +    @Volatile private var pendingSpeedMultiplier: Float = 1f   
      30      private var job: Job? = null
      31  
      32      val isRunning: Boolean get() = pending != null
     ...
      37       * at a time; calling this while one is in flight is a no-o
          p.
      38       *
      39       * Pass [countAsHint] = false when the player triggered the
           action
      39 -     * directly (e.g., the Sweep button) so it doesn't bump hin
         -tCount.                                                        
      40 +     * directly (e.g., the Sweep button or "Show Again") so it 
         +doesn't                                                        
      41 +     * bump hintCount. [speedMultiplier] scales every per-step 
         +delay; use                                                     
      42 +     * a value > 1 to play the animation slower than the config
         +ured pace                                                      
      43 +     * (Show Again).                                           
      44       */
      41 -    fun launch(hint: HintEngine.Hint, countAsHint: Boolean = tr
         -ue): Job? {                                                    
      45 +    fun launch(                                                
      46 +        hint: HintEngine.Hint,                                 
      47 +        countAsHint: Boolean = true,                           
      48 +        speedMultiplier: Float = 1f,                           
      49 +    ): Job? {                                                  
      50          if (pending != null) return job
      51          pending = hint
      52          pendingCountAsHint = countAsHint
      53 +        pendingSpeedMultiplier = speedMultiplier               
      54          val j = scope.launch {
      55              try {
      56                  when (hint) {
      48 -                    is HintEngine.NakedSingle -> runNakedSingle
         -(hint)                                                         
      49 -                    is HintEngine.HiddenSingle -> runHiddenSing
         -le(hint)                                                       
      57 +                    is HintEngine.NakedSingle -> runNakedSingle
         +(hint, countAsHint)                                            
      58 +                    is HintEngine.HiddenSingle -> runHiddenSing
         +le(hint, countAsHint)                                          
      59                      is HintEngine.CandidateSweep -> runCandidat
          eSweep(hint, countAsHint)
      51 -                    is HintEngine.EliminationHint -> runElimina
         -tionHint(hint)                                                 
      60 +                    is HintEngine.EliminationHint -> runElimina
         +tionHint(hint, countAsHint)                                    
      61                  }
      62              } finally {
      63                  if (pending === hint) pending = null
     ...
      79          setState(complete(getState(), h, count))
      80      }
      81  
      82 +    private fun stepMs(): Long =                               
      83 +        (stepDelayMs() * pendingSpeedMultiplier).toLong().coerc
         +eAtLeast(1)                                                    
      84 +                                                               
      85 +    private fun sweepMs(): Long =                              
      86 +        (sweepCellMs() * pendingSpeedMultiplier).toLong().coerc
         +eAtLeast(1)                                                    
      87 +                                                               
      88      /** Apply every remaining step of [hint] and finish it, ide
          mpotently. */
      89      private fun complete(
      90          state: GameState,

● Update the runners to use the scaled delays and pass countAsHint:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 18 lines, removed 18 lines
      121          }
      122      }
      123  
      124 -    private suspend fun runNakedSingle(hint: HintEngine.NakedS
          -ingle) {                                                      
      124 +    private suspend fun runNakedSingle(hint: HintEngine.NakedS
          +ingle, countAsHint: Boolean) {                                
      125          setState(GameReducer.beginNakedSingleHint(getState(), 
           hint))
      126 -        delay(stepDelayMs())                                  
      126 +        delay(stepMs())                                       
      127          for (d in 1..9) {
      128              setState(GameReducer.advanceNakedSingleStep(
      129                  state = getState(),
     ...
      131                  eliminatorCells = hint.eliminators[d].orEmpty(
           ),
      132                  eliminate = d != hint.solutionDigit,
      133              ))
      134 -            delay(stepDelayMs())                              
      134 +            delay(stepMs())                                   
      135          }
      136 -        setState(GameReducer.completeNakedSingleHint(getState(
          -), hint))                                                     
      136 +        setState(GameReducer.completeNakedSingleHint(getState(
          +), hint, countAsHint = countAsHint))                          
      137      }
      138  
      139 -    private suspend fun runHiddenSingle(hint: HintEngine.Hidde
          -nSingle) {                                                    
      139 +    private suspend fun runHiddenSingle(hint: HintEngine.Hidde
          +nSingle, countAsHint: Boolean) {                              
      140          setState(GameReducer.beginHiddenSingleHint(getState(),
            hint))
      141 -        delay(stepDelayMs())                                  
      141 +        delay(stepMs())                                       
      142          for (cellIdx in hint.unit.cells) {
      143              if (cellIdx == hint.targetCell) continue
      144              if (getState().cells[cellIdx].value != null) conti
           nue
     ...
      147                  currentCell = cellIdx,
      148                  eliminatorCells = hint.eliminators[cellIdx].or
           Empty(),
      149              ))
      150 -            delay(stepDelayMs())                              
      150 +            delay(stepMs())                                   
      151          }
      152          setState(GameReducer.advanceHiddenSingleStep(
      153              state = getState(),
      154              currentCell = null,
      155              eliminatorCells = emptySet(),
      156          ))
      157 -        delay(stepDelayMs())                                  
      158 -        setState(GameReducer.completeHiddenSingleHint(getState
          -(), hint))                                                    
      157 +        delay(stepMs())                                       
      158 +        setState(GameReducer.completeHiddenSingleHint(getState
          +(), hint, countAsHint = countAsHint))                         
      159      }
      160  
      161 -    private suspend fun runEliminationHint(hint: HintEngine.El
          -iminationHint) {                                              
      161 +    private suspend fun runEliminationHint(hint: HintEngine.El
          +iminationHint, countAsHint: Boolean) {                        
      162          setState(GameReducer.beginEliminationHint(getState(), 
           hint))
      163 -        delay(stepDelayMs())                                  
      163 +        delay(stepMs())                                       
      164          for (step in hint.plan) {
      165              setState(GameReducer.advanceEliminationStep(
      166                  state = getState(),
     ...
      168                  doomedDigits = step.doomedDigits,
      169                  explainerCells = step.explainerCells,
      170              ))
      171 -            delay(sweepCellMs())                              
      171 +            delay(sweepMs())                                  
      172          }
      173          setState(GameReducer.advanceEliminationStep(
      174              state = getState(),
     ...
      176              doomedDigits = emptySet(),
      177              explainerCells = emptySet(),
      178          ))
      179 -        delay(stepDelayMs())                                  
      180 -        setState(GameReducer.completeEliminationHint(getState(
          -), hint))                                                     
      179 +        delay(stepMs())                                       
      180 +        setState(GameReducer.completeEliminationHint(getState(
          +), hint, countAsHint = countAsHint))                          
      181      }
      182  
      183      private suspend fun runCandidateSweep(hint: HintEngine.Can
           didateSweep, countAsHint: Boolean) {
      184          setState(GameReducer.beginCandidateSweep(getState(), h
           int))
      185 -        delay(stepDelayMs())                                  
      185 +        delay(stepMs())                                       
      186          if (hint.initialFill) {
      187              setState(GameReducer.applyCandidateSweepFill(getSt
           ate()))
      188 -            delay(stepDelayMs())                              
      188 +            delay(stepMs())                                   
      189          }
      190          for (plan in hint.plan) {
      191              setState(GameReducer.advanceCandidateSweepCell(
     ...
      194                  doomedDigits = plan.doomedDigits,
      195                  eliminatorPeers = plan.eliminatorPeers,
      196              ))
      197 -            delay(sweepCellMs())                              
      197 +            delay(sweepMs())                                  
      198          }
      199          setState(GameReducer.advanceCandidateSweepCell(
      200              state = getState(),
     ...
      202              doomedDigits = emptySet(),
      203              eliminatorPeers = emptySet(),
      204          ))
      205 -        delay(stepDelayMs())                                  
      205 +        delay(stepMs())                                       
      206          setState(GameReducer.completeCandidateSweep(getState()
           , hint, countAsHint = countAsHint))
      207      }
      208  }

● Config + ViewModel + button:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      11      val hintFlashMs: Long = 600,
      12      val sweepCellMs: Long = 150,
      13      val pendingHintMs: Long = 60_000,
      14 +    val showAgainMultiplier: Float = 3f,                       
      15      val showTestButton: Boolean = true,
      16  ) {
      17      companion object {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line
      40                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      41                  sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
          l() ?: 150,
      42                  pendingHintMs = map["pending_hint_ms"]?.toLongO
          rNull() ?: 60_000,
      43 +                showAgainMultiplier = map["show_again_multiplie
         +r"]?.toFloatOrNull() ?: 3f,                                    
      44                  showTestButton = map["show_test_button"]?.toBoo
          leanStrictOrNull() ?: true,
      45              )
      46          }

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 5 lines
      36  #                  that advances the stage.
      37  pending_hint_ms: 60000
      38  
      39 +# show_again_multiplier: How much slower the Show Again button 
         +replays                                                        
      40 +#                        the last hint compared to the normal a
         +nimation                                                       
      41 +#                        pace. 3.0 = three times as slow.      
      42 +show_again_multiplier: 3.0                                     
      43 +                                                               
      44  # show_test_button: When true, the main menu shows a "Test" but
          ton below
      45  #                   Extreme that walks through each hint techni
          que's
      46  #                   teaching animation on a pre-built example p
          uzzle.

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 27 lines
       92          }
       93      }
       94  
       95 +    fun onShowAgain() {                                       
       96 +        val s = _state.value                                  
       97 +        if (s.phase != GameState.Phase.Playing) return        
       98 +        if (animator.isRunning) return                        
       99 +        val hint = s.lastHint ?: return                       
      100 +        // If the last history entry is still the hint's own m
          +ove, undo                                                     
      101 +        // it so the animation has something to apply. (If the
          + player                                                       
      102 +        // already pressed Undo manually, the hint isn't on th
          +e board                                                       
      103 +        // anymore — just re-apply.)                          
      104 +        val needsUndo = hintMatchesTopOfHistory(s, hint)      
      105 +        val base = if (needsUndo) GameReducer.undo(s) else s  
      106 +        _state.value = base.copy(lastHint = hint)             
      107 +        animator.launch(                                      
      108 +            hint = hint,                                      
      109 +            countAsHint = false,                              
      110 +            speedMultiplier = config.showAgainMultiplier,     
      111 +        )                                                     
      112 +    }                                                         
      113 +                                                              
      114 +    private fun hintMatchesTopOfHistory(state: GameState, hint
          +: HintEngine.Hint): Boolean {                                 
      115 +        val last = state.history.lastOrNull() ?: return false 
      116 +        return when (hint) {                                  
      117 +            is HintEngine.NakedSingle, is HintEngine.HiddenSin
          +gle -> last is Move.SetValue                                  
      118 +            else -> last is Move.PencilSweep                  
      119 +        }                                                     
      120 +    }                                                         
      121 +                                                              
      122      fun onSweep() {
      123          val s = _state.value
      124          if (s.phase != GameState.Phase.Playing) return
● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 1 line
      12  import com.dividetask.sudokutrainer.domain.GuessColor
      13  import com.dividetask.sudokutrainer.domain.HintEngine
      14  import com.dividetask.sudokutrainer.domain.HintUi
      15 +import com.dividetask.sudokutrainer.domain.Move                
      16  import kotlinx.coroutines.Job
      17  import kotlinx.coroutines.delay
      18  import kotlinx.coroutines.flow.MutableStateFlow

● Add the button to ActionRow:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 2 lines
      180      onClear: () -> Unit,
      181      onSweep: () -> Unit,
      182      onHint: () -> Unit,
      183 +    onShowAgain: () -> Unit,                                  
      184      onGiveUpOrSolve: () -> Unit,
      185      modifier: Modifier = Modifier,
      186  ) {
     ...
      190      // The hint button stays enabled while a hint is animating
            so pressing
      191      // it again skips the animation to the final placement.
      192      val canHint = playing || state.phase == GameState.Phase.Hi
           nting
      193 +    val canShowAgain = playing && state.lastHint != null      
      194  
      195      Column(
      196          modifier = modifier.fillMaxWidth(),

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Controls.kt)
  ⎿  Added 10 lines
      233              horizontalArrangement = Arrangement.spacedBy(6.dp)
           ,
      234          ) {
      235              Button(
      236 +                onClick = onShowAgain,                        
      237 +                enabled = canShowAgain,                       
      238 +                modifier = Modifier.weight(1f),               
      239 +                colors = ButtonDefaults.buttonColors(         
      240 +                    containerColor = Color(0xFF6A1B9A),       
      241 +                ),                                            
      242 +            ) {                                               
      243 +                Text("Show Again")                            
      244 +            }                                                 
      245 +            Button(                                           
      246                  onClick = onClear,
      247                  enabled = playing,
      248                  modifier = Modifier.weight(1f),

● Wire it into GameScreen:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
      68                      onClear = viewModel::onClear,
      69                      onSweep = viewModel::onSweep,
      70                      onHint = viewModel::onHint,
      71 +                    onShowAgain = viewModel::onShowAgain,      
      72                      onGiveUpOrSolve = {
      73                          if (viewModel.isReadyToSolve) viewModel
          .onSolve() else viewModel.onGiveUp()
      74                      },

● Add a domain test that locks in the lastHint lifecycle:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 70 lines
      739      }
      740  
      741      @Test
      742 +    fun `lastHint is set on completion and survives an undo`()
          + {                                                            
      743 +        val s0 = initial()                                    
      744 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      745 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      746 +                                                              
      747 +        var s = GameReducer.beginNakedSingleHint(s0, hint)    
      748 +        for (d in 1..9) {                                     
      749 +            s = GameReducer.advanceNakedSingleStep(           
      750 +                state = s,                                    
      751 +                activeDigit = d,                              
      752 +                eliminatorCells = hint.eliminators[d].orEmpty(
          +),                                                            
      753 +                eliminate = d != hint.solutionDigit,          
      754 +            )                                                 
      755 +        }                                                     
      756 +        s = GameReducer.completeNakedSingleHint(s, hint)      
      757 +        assertEquals(hint, s.lastHint)                        
      758 +                                                              
      759 +        // Undo pops the hint move but keeps lastHint set so S
          +how Again works.                                              
      760 +        s = GameReducer.undo(s)                               
      761 +        assertEquals(hint, s.lastHint)                        
      762 +    }                                                         
      763 +                                                              
      764 +    @Test                                                     
      765 +    fun `lastHint clears on any player cell change`() {       
      766 +        val s0 = initial()                                    
      767 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      768 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      769 +                                                              
      770 +        var s = GameReducer.beginNakedSingleHint(s0, hint)    
      771 +        for (d in 1..9) {                                     
      772 +            s = GameReducer.advanceNakedSingleStep(           
      773 +                state = s,                                    
      774 +                activeDigit = d,                              
      775 +                eliminatorCells = hint.eliminators[d].orEmpty(
          +),                                                            
      776 +                eliminate = d != hint.solutionDigit,          
      777 +            )                                                 
      778 +        }                                                     
      779 +        s = GameReducer.completeNakedSingleHint(s, hint)      
      780 +        assertEquals(hint, s.lastHint)                        
      781 +                                                              
      782 +        // Find any non-given empty cell and place a value the
          +re.                                                           
      783 +        val targetEmpty = (0 until 81).first {                
      784 +            !s.cells[it].given && s.cells[it].value == null   
      785 +        }                                                     
      786 +        s = GameReducer.selectDigit(s, 1)                     
      787 +        s = GameReducer.tapCell(s, targetEmpty / 9, targetEmpt
          +y % 9)                                                        
      788 +        assertNull(s.lastHint, "any cell change must clear las
          +tHint")                                                       
      789 +    }                                                         
      790 +                                                              
      791 +    @Test                                                     
      792 +    fun `completion with countAsHint false leaves hintCount un
          +changed but sets lastHint`() {                                
      793 +        val s0 = initial()                                    
      794 +        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          +}                                                             
      795 +        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          +ngle                                                          
      796 +                                                              
      797 +        var s = GameReducer.beginNakedSingleHint(s0, hint)    
      798 +        for (d in 1..9) {                                     
      799 +            s = GameReducer.advanceNakedSingleStep(           
      800 +                state = s,                                    
      801 +                activeDigit = d,                              
      802 +                eliminatorCells = hint.eliminators[d].orEmpty(
          +),                                                            
      803 +                eliminate = d != hint.solutionDigit,          
      804 +            )                                                 
      805 +        }                                                     
      806 +        s = GameReducer.completeNakedSingleHint(s, hint, count
          +AsHint = false)                                               
      807 +        assertEquals(0, s.hintCount)                          
      808 +        assertEquals(hint, s.lastHint)                        
      809 +    }                                                         
      810 +                                                              
      811 +    @Test                                                     
      812      fun `resetBoard returns to starting position`() {
      813          var s = initial()
      814          s = GameReducer.selectDigit(s, 4)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -15)
  ⎿  Daemon will be stopped at the end of the build
     > Task :domain:checkKotlinGradlePluginConfigurationErrors SKIPPED
     > Task :domain:processResources NO-SOURCE
     … +12 lines (ctrl+o to expand)

● Update the testing docs to mention the new tests, then commit:

● Update(/home/user/soduku-trainer/docs/testing/reducer-tests.md)
  ⎿  Added 12 lines
      63    fill the board correctly, assert `isMostlySolved(10)`; introd
          uce
      64    one wrong digit, assert it becomes false.
      65  
      66 +## Show Again / lastHint                                       
      67 +                                                               
      68 +- **`lastHint is set on completion and survives an undo`** — af
         +ter a                                                          
      69 +  hint animation `lastHint` equals the hint object; pressing Un
         +do                                                             
      70 +  pops the hint's move but keeps `lastHint` so Show Again can r
         +eplay.                                                         
      71 +- **`lastHint clears on any player cell change`** — once the pl
         +ayer                                                           
      72 +  places a value (or tap-clears one) Show Again should disable,
         + so                                                            
      73 +  `lastHint` must become null on that move.                    
      74 +- **`completion with countAsHint false leaves hintCount unchang
         +ed but                                                         
      75 +  sets lastHint`** — the Show Again replay path and the Sweep b
         +utton                                                          
      76 +  both rely on this flag.                                      
      77 +                                                               
      78  ## Reset
      79  
      80  - **`resetBoard returns to starting position`** — history wiped
          ,

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Show Again button for replaying the last hint slowly…)
  ⎿  [claude/add-sudoku-hint-button-L1spT e7b6697] Add Show Again button fo
     r replaying the last hint slowly
      10 files changed, 205 insertions(+), 35 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed a9b9c75.

  Behavior:
  - After any hint animation finishes, GameState.lastHint holds the underlying
  HintEngine.Hint. The purple Show Again button (bottom row, between Clear and
  Give Up) is enabled iff lastHint != null and the game is in Playing phase.
  - Pressing Show Again: undoes the hint's move (if it's still the top of
  history), then re-launches the animator with countAsHint = false and a
  speedMultiplier (default 3x) so hintCount doesn't double-count and the
  animation is visibly slower.
  - lastHint clears when the player makes any cell-changing action (tap places a
  value, pencil toggle, double-tap clear). It does not clear on undo, so the
  sequence Hint → Undo → Show Again works.
  - New config: show_again_multiplier: 3.0 in config.yaml.

  Tests added (GameReducerTest.kt):
  - lastHint is set on completion and survives an undo
  - lastHint clears on any player cell change
  - completion with countAsHint false leaves hintCount unchanged but sets 
  lastHint

  Updated docs/testing/reducer-tests.md with the new "Show Again / lastHint"
  section.

❯ We need another hint                                                          
  This should be right before the hint that fills out the candidates. If a cell 
  has only one candidate listed as a possibility then it should fill that       
  candidate in                                                                  

● Adding a Note Single technique that fires between Hidden Single and Candidate
  Sweep — when a cell has exactly one pencil mark and that mark is still a legal
  candidate, fill it in.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 16 lines
      51      }
      52  
      53      /**
      54 +     * The player has narrowed a cell's pencil marks down to a 
         +single                                                         
      55 +     * digit (typically by applying earlier eliminations). Plac
         +e it.                                                          
      56 +     *                                                         
      57 +     * Fires only when [HintEngine.find] is called with non-nul
         +l                                                              
      58 +     * `marks`, after Naked/Hidden Single (which work off grid 
         +values                                                         
      59 +     * alone) but before Candidate Sweep (which would rewrite t
         +he                                                             
      60 +     * notes entirely).                                        
      61 +     */                                                        
      62 +    data class NoteSingle(                                     
      63 +        val targetCell: Int,                                   
      64 +        val solutionDigit: Int,                                
      65 +    ) : Hint {                                                 
      66 +        override val techniqueName: String = "Note Single"     
      67 +    }                                                          
      68 +                                                               
      69 +    /**                                                        
      70       * Prune impossible pencil marks from every empty cell — an
          y candidate
      71       * digit blocked by a peer in the same row, column, or box 
          is removed.
      72       * When the board has no player notes at all, the sweep als
          o *adds* the

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 1 line
      237          require(marks == null || marks.size == 81)
      238          findNakedSingle(grid)?.let { return it }
      239          findHiddenSingle(grid)?.let { return it }
      240 +        findNoteSingle(grid, marks)?.let { return it }        
      241          findCandidateSweepInternal(grid, marks)?.let { return 
           it }
      242          val cands = candidateGrid(grid, marks)
      243          findNakedPair(grid, cands)?.let { return it }

● Now add the finder. Inserting next to findCandidateSweepInternal:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngine.kt)
  ⎿  Added 22 lines
      330          return findCandidateSweepInternal(grid, marks)
      331      }
      332  
      333 +    /**                                                       
      334 +     * Walks empty cells and reports the first one whose playe
          +r marks are                                                   
      335 +     * exactly one digit (and that digit is still a legal plac
          +ement). The                                                   
      336 +     * lone mark *might* be smaller than the grid-computed can
          +didate set,                                                   
      337 +     * which is why this fires between Hidden Single and Candi
          +date Sweep                                                    
      338 +     * — the player has done elimination work that grid values
          + alone                                                        
      339 +     * don't reflect.                                         
      340 +     */                                                       
      341 +    internal fun findNoteSingle(grid: IntArray, marks: Array<S
          +et<Int>>?): NoteSingle? {                                     
      342 +        if (marks == null) return null                        
      343 +        for (idx in 0 until 81) {                             
      344 +            if (grid[idx] != 0) continue                      
      345 +            val cellMarks = marks[idx]                        
      346 +            if (cellMarks.size != 1) continue                 
      347 +            val d = cellMarks.first()                         
      348 +            val r = idx / 9; val c = idx % 9                  
      349 +            if (!SudokuSolver.isValidPlacement(grid, r, c, d))
          + continue                                                     
      350 +            return NoteSingle(targetCell = idx, solutionDigit 
          += d)                                                          
      351 +        }                                                     
      352 +        return null                                           
      353 +    }                                                         
      354 +                                                              
      355      private fun findCandidateSweepInternal(grid: IntArray, mar
           ks: Array<Set<Int>>?): CandidateSweep? {
      356          val anyMarks = marks != null && marks.any { it.isNotEm
           pty() }
      357          val initialFill = !anyMarks

● Add HintUi.NoteSingle, the reducer methods, and the animator runner.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/HintUi.kt)
  ⎿  Added 13 lines
      65       * - [priorMarks] is the full pencil-mark snapshot captured
           at begin;
      66       *   completion uses it to build the consolidated undo move
          .
      67       */
      68 +    /**                                                        
      69 +     * Note-single teaching animation in progress. Just frames 
         +the                                                            
      70 +     * target cell green and pauses; the lone pencil mark is al
         +ready                                                          
      71 +     * what the player sees.                                   
      72 +     *                                                         
      73 +     * [originalNotes] captures pre-hint marks for undo.       
      74 +     */                                                        
      75 +    data class NoteSingle(                                     
      76 +        val targetCell: Int,                                   
      77 +        val solutionDigit: Int,                                
      78 +        val originalNotes: Map<Int, GuessColor> = emptyMap(),  
      79 +    ) : HintUi                                                 
      80 +                                                               
      81      data class CandidateSweep(
      82          val initialFill: Boolean,
      83          val currentCell: Int? = null,

● Add the reducer methods. I'll insert just before beginCandidateSweep:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 45 lines
      428          )
      429      }
      430  
      431 +    // ---- Note-single hint animation ----                   
      432 +                                                              
      433 +    fun beginNoteSingleHint(state: GameState, hint: HintEngine
          +.NoteSingle): GameState {                                     
      434 +        val target = state.cells[hint.targetCell]             
      435 +        return state.copy(                                    
      436 +            phase = GameState.Phase.Hinting,                  
      437 +            activeDigit = null,                               
      438 +            hintUi = HintUi.NoteSingle(                       
      439 +                targetCell = hint.targetCell,                 
      440 +                solutionDigit = hint.solutionDigit,           
      441 +                originalNotes = target.pencilMarks,           
      442 +            ),                                                
      443 +        )                                                     
      444 +    }                                                         
      445 +                                                              
      446 +    fun completeNoteSingleHint(                               
      447 +        state: GameState,                                     
      448 +        hint: HintEngine.NoteSingle,                          
      449 +        countAsHint: Boolean = true,                          
      450 +    ): GameState {                                            
      451 +        val ui = state.hintUi as? HintUi.NoteSingle           
      452 +        val target = state.cells[hint.targetCell]             
      453 +        val placed = target.copy(                             
      454 +            value = hint.solutionDigit,                       
      455 +            valueColor = CellColor.Guess(GuessColor.Blue),    
      456 +            pencilMarks = emptyMap(),                         
      457 +        )                                                     
      458 +        val move = Move.SetValue(                             
      459 +            row = target.row,                                 
      460 +            col = target.col,                                 
      461 +            newValue = hint.solutionDigit,                    
      462 +            newValueColor = GuessColor.Blue,                  
      463 +            clearedMarks = ui?.originalNotes.orEmpty(),       
      464 +        )                                                     
      465 +        return state                                          
      466 +            .replaceCell(placed)                              
      467 +            .copy(                                            
      468 +                phase = GameState.Phase.Playing,              
      469 +                hintUi = null,                                
      470 +                hintCount = if (countAsHint) state.hintCount +
          + 1 else state.hintCount,                                      
      471 +                history = state.history + move,               
      472 +                lastHint = hint,                              
      473 +            )                                                 
      474 +    }                                                         
      475 +                                                              
      476      // ---- Candidate-sweep hint animation ----
      477  
      478      /**

● Wire it into HintAnimator:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)  ⎿  Added 1 line
      56                  when (hint) {
      57                      is HintEngine.NakedSingle -> runNakedSingle
          (hint, countAsHint)
      58                      is HintEngine.HiddenSingle -> runHiddenSing
          le(hint, countAsHint)
      59 +                    is HintEngine.NoteSingle -> runNoteSingle(h
         +int, countAsHint)                                              
      60                      is HintEngine.CandidateSweep -> runCandidat
          eSweep(hint, countAsHint)
      61                      is HintEngine.EliminationHint -> runElimina
          tionHint(hint, countAsHint)
      62                  }

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 3 lines, removed 2 lines
       92          hint: HintEngine.Hint,
       93          countAsHint: Boolean,
       94      ): GameState = when (hint) {
       95 -        is HintEngine.NakedSingle -> GameReducer.completeNaked
          -SingleHint(state, hint)                                       
       96 -        is HintEngine.HiddenSingle -> GameReducer.completeHidd
          -enSingleHint(state, hint)                                     
       95 +        is HintEngine.NakedSingle -> GameReducer.completeNaked
          +SingleHint(state, hint, countAsHint = countAsHint)            
       96 +        is HintEngine.HiddenSingle -> GameReducer.completeHidd
          +enSingleHint(state, hint, countAsHint = countAsHint)          
       97 +        is HintEngine.NoteSingle -> GameReducer.completeNoteSi
          +ngleHint(state, hint, countAsHint = countAsHint)              
       98          is HintEngine.CandidateSweep -> {
       99              var s = state
      100              // Re-applying these is idempotent — finished cell
           s skip.

● Add the runner — short, two pauses around the placement:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/HintAnimator.kt)
  ⎿  Added 9 lines
      138          setState(GameReducer.completeNakedSingleHint(getState(
           ), hint, countAsHint = countAsHint))
      139      }
      140  
      141 +    private suspend fun runNoteSingle(hint: HintEngine.NoteSin
          +gle, countAsHint: Boolean) {                                  
      142 +        setState(GameReducer.beginNoteSingleHint(getState(), h
          +int))                                                         
      143 +        // Two pauses so the player has a beat to recognize th
          +e cell and                                                    
      144 +        // its lone candidate before the value drops in.      
      145 +        delay(stepMs())                                       
      146 +        delay(stepMs())                                       
      147 +        setState(GameReducer.completeNoteSingleHint(getState()
          +, hint, countAsHint = countAsHint))                           
      148 +    }                                                         
      149 +                                                              
      150      private suspend fun runHiddenSingle(hint: HintEngine.Hidde
           nSingle, countAsHint: Boolean) {
      151          setState(GameReducer.beginHiddenSingleHint(getState(),
            hint))
      152          delay(stepMs())

● Render in Board and Label, and route through the pending-hint flow in the
  ViewModel:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 11 lines
      200                          }
      201                      }
      202  
      203 +                    // Note-single overlay: just the green tar
          +get cell.                                                     
      204 +                    if (hint is HintUi.NoteSingle) {          
      205 +                        val r = hint.targetCell / 9           
      206 +                        val c = hint.targetCell % 9           
      207 +                        drawRect(                             
      208 +                            color = targetBg,                 
      209 +                            topLeft = Offset(c * cellW, r * ce
          +llH),                                                         
      210 +                            size = Size(cellW, cellH),        
      211 +                        )                                     
      212 +                    }                                         
      213 +                                                              
      214                      // Candidate-sweep overlay: amber cursor +
            red
      215                      // eliminator peers for the cell being pru
           ned.
      216                      if (hint is HintUi.CandidateSweep) {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line
       94      val text = when (ui) {
       95          is HintUi.NakedSingle -> "Naked Single"
       96          is HintUi.HiddenSingle -> "Hidden Single"
       97 +        is HintUi.NoteSingle -> "Note Single"                 
       98          is HintUi.CandidateSweep -> "Candidate Sweep"
       99          is HintUi.Elimination -> ui.techniqueName
      100          HintUi.NoHintFlash -> "No hint available"

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 4 lines
      184                  _state.value = GameReducer.setPendingHint(s, h
           int, setOf(hint.targetCell))
      185                  startPendingHintTimer()
      186              }
      187 +            is HintEngine.NoteSingle -> {                     
      188 +                _state.value = GameReducer.setPendingHint(s, h
          +int, setOf(hint.targetCell))                                  
      189 +                startPendingHintTimer()                       
      190 +            }                                                 
      191              is HintEngine.CandidateSweep -> {
      192                  // No single target cell, so no progressive fl
           ow — animate now.
      193                  animator.launch(hint)

● Add a domain test:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 40 lines
      41      }
      42  
      43      @Test
      44 +    fun `Note Single fires when a cell's marks are a single leg
         +al digit`() {                                                  
      45 +        // Grid with no naked or hidden singles available, but 
         +cell (0,0)                                                     
      46 +        // has only digit 5 in its player marks. Engine should 
         +return                                                         
      47 +        // NoteSingle for that cell (not naked/hidden single). 
      48 +        val grid = IntArray(81)                                
      49 +        // Place enough peers so (0,0) is plausibly legal for 5
         + but has                                                       
      50 +        // many grid-derived candidates — the deduction relies 
         +on marks.                                                      
      51 +        val marks = Array(81) { idx ->                         
      52 +            if (idx == 0) setOf(5) else (1..9).filter {        
      53 +                SudokuSolver.isValidPlacement(grid, idx / 9, id
         +x % 9, it)                                                     
      54 +            }.toSet()                                          
      55 +        }                                                      
      56 +        val hint = HintEngine.find(grid, marks)                
      57 +        assertTrue(hint is HintEngine.NoteSingle, "expected Not
         +eSingle, got $hint")                                           
      58 +        hint as HintEngine.NoteSingle                          
      59 +        assertEquals(0, hint.targetCell)                       
      60 +        assertEquals(5, hint.solutionDigit)                    
      61 +    }                                                          
      62 +                                                               
      63 +    @Test                                                      
      64 +    fun `Note Single ignores a single mark that's no longer leg
         +al`() {                                                        
      65 +        // Cell (0,0) has mark {5}, but 5 is already placed at 
         +(0,1) so                                                       
      66 +        // 5 isn't a legal placement at (0,0). Engine must skip
         + Note Single.                                                  
      67 +        val grid = IntArray(81)                                
      68 +        grid[0 * 9 + 1] = 5                                    
      69 +        val marks = Array(81) { idx ->                         
      70 +            when (idx) {                                       
      71 +                0 -> setOf(5)                                  
      72 +                in (1..80) -> if (grid[idx] != 0) emptySet() el
         +se (1..9).filter {                                             
      73 +                    SudokuSolver.isValidPlacement(grid, idx / 9
         +, idx % 9, it)                                                 
      74 +                }.toSet()                                      
      75 +                else -> emptySet()                             
      76 +            }                                                  
      77 +        }                                                      
      78 +        val hint = HintEngine.find(grid, marks)                
      79 +        assertFalse(hint is HintEngine.NoteSingle,             
      80 +            "engine must not return NoteSingle when the lone ma
         +rk is illegal")                                                
      81 +    }                                                          
      82 +                                                               
      83 +    @Test                                                      
      84      fun `empty grid falls through to a fill-only Candidate Swee
          p`() {
      85          // No givens, no notes: no single applies, but seeding 
          candidates
      86          // is itself work — CandidateSweep with initialFill = t
          rue.

  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/HintEngineTest.kt)
  ⎿  Added 1 line
      1  package com.dividetask.sudokutrainer.domain
      2  
      3  import org.junit.jupiter.api.Assertions.assertEquals
      4 +import org.junit.jupiter.api.Assertions.assertFalse             
      5  import org.junit.jupiter.api.Assertions.assertNotNull
      6  import org.junit.jupiter.api.Assertions.assertNull
      7  import org.junit.jupiter.api.Assertions.assertTrue

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● Update(/home/user/soduku-trainer/docs/testing/hint-engine-tests.md)
  ⎿  Added 7 lines
      29  - **`prefers naked single over hidden single when both apply`**
           —
      30    same setup as the naked-single test but also a hidden single 
          for 9.
      31    Naked single must win.
      32 +- **`Note Single fires when a cell's marks are a single legal d
         +igit`**                                                        
      33 +  — the player has narrowed a cell to one pencil mark (e.g. via
         + an                                                            
      34 +  earlier pair). Engine must classify it as `NoteSingle` even t
         +hough                                                          
      35 +  the grid alone allows many candidates.                       
      36 +- **`Note Single ignores a single mark that's no longer legal`*
         +* —                                                            
      37 +  if the player's lone mark conflicts with a peer's placed valu
         +e,                                                             
      38 +  Note Single must not fire (Candidate Sweep will tidy it later
         +).                                                             
      39  
      40  ## Candidate Sweep
      41  

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Add Note Single hint: fill in a cell whose pencil marks list one digit…)
  ⎿  [claude/add-sudoku-hint-button-L1spT fb3ca2a] Add Note Single hint: fi
     ll in a cell whose pencil marks list one digit
      9 files changed, 174 insertions(+), 2 deletions(-)
     … +6 lines (ctrl+o to expand)

● Pushed fb3ca2a.

  New cascade order: NakedSingle → HiddenSingle → NoteSingle → CandidateSweep → 
  ...

  When Note Single fires: the engine call has non-null marks, a non-given empty
  cell has exactly one pencil mark, and that digit is still a legal placement at
  the cell. The deduction often follows pair/triple elimination — the cell's
  grid-derived candidates may still be plural but the player's notes have
  narrowed it to one.

  Behavior:
  - Engine returns HintEngine.NoteSingle(targetCell, solutionDigit).
  - Progressive Hint flow with keyCells = {targetCell} (same as Naked/Hidden
  Single).
  - Animation: green cell, two short pauses, then the value drops in Blue.
  No 1..9 sweep — the player already sees the lone candidate, so there's nothing
  to enumerate.
  - Completion emits a Move.SetValue so undo restores the original pencil marks;
  lastHint is set so Show Again can replay it.

  Tests added:
  - Note Single fires when a cell's marks are a single legal digit — fires when
  grid-derived candidates would be many but marks have size 1.
  - Note Single ignores a single mark that's no longer legal — guards against
  placing a value that contradicts a peer.

  Docs updated in docs/testing/hint-engine-tests.md.

❯ After hitting Hint, the hint should disappear after a minute. The hint should 
  also disappear once any squares are solved or notes are changed.              
                                                                                
  Can we improve the fireworks at all? Make them larger or make the animation   
  look better?                                                                  

● Let me check the firework code and the current pendingHint behavior.

  Read 2 files (ctrl+o to expand)

● Make pendingHint clear on any cell change (any value placed, any note toggled,
  any double-tap clear):

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 8 lines, removed 10 lines
      72              valueColor = CellColor.Guess(state.activeColor),
      73              pencilMarks = emptyMap(),
      74          )
      75 -        // Placing a value in any of the pending-hint key cells
         - cancels it,                                                   
      76 -        // and any board-changing player action invalidates "Sh
         -ow Again".                                                     
      77 -        val idx = cell.row * 9 + cell.col                      
      78 -        var cleared = s.copy(lastHint = null)                  
      79 -        if (cleared.pendingHint?.keyCells?.contains(idx) == tru
         -e) {                                                           
      80 -            cleared = cleared.copy(pendingHint = null)         
      81 -        }                                                      
      82 -        return cleared.replaceCell(newCell).appendHistory(move)
      75 +        // Any board-changing player action invalidates the pen
         +ding hint                                                      
      76 +        // and the Show Again button — clears them both uncondi
         +tionally.                                                      
      77 +        return s                                               
      78 +            .copy(lastHint = null, pendingHint = null)         
      79 +            .replaceCell(newCell)                              
      80 +            .appendHistory(move)                               
      81      }
      82  
      83      private fun tapCellPencilOn(state: GameState, cell: Cell, d
          igit: Int): GameState {
     ...
      106              m to move
      107          }
      108          return state
      111 -            .copy(lastHint = null)                            
      109 +            .copy(lastHint = null, pendingHint = null)        
      110              .replaceCell(cell.copy(pencilMarks = newMarks))
      111              .appendHistory(move)
      112      }
     ...
      127          )
      128          val newCell = cell.copy(value = null, valueColor = nul
           l)
      129          return state
      132 -            .copy(lastHint = null)                            
      130 +            .copy(lastHint = null, pendingHint = null)        
      131              .replaceCell(newCell)
      132              .appendHistory(move)
      133      }

● Update the test expectations — the "non-key cell preserves hint" tests now
  need to flip:

  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 21 lines, removed 31 lines
      690      }
      691  
      692      @Test
      693 -    fun `pending hint covers multi-cell patterns`() {         
      694 -        // Use a fake hint with multiple keyCells so we exerci
          -se the                                                        
      695 -        // "any cell in keyCells clears it" branch of tapCell.
      693 +    fun `any cell change clears the pending hint`() {         
      694 +        // Whether the player taps a key cell or a different c
          +ell — and                                                     
      695 +        // whether it's a value placement, a pencil toggle, or
          + a                                                            
      696 +        // double-tap clear — the pending hint must disappear.
      697          val s0 = initial()
      698          val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
           }
      699          val hint = HintEngine.find(grid) as HintEngine.NakedSi
           ngle
      699 -        // Pick three non-given empty cells as a multi-cell pa
          -ttern.                                                        
      700          val emptyIndices = (0 until 81).filter { !s0.cells[it]
           .given && s0.cells[it].value == null }
      701          val keys = emptyIndices.take(3).toSet()
      702          check(keys.size == 3)
      703 -        val otherEmpty = emptyIndices.first { it !in keys }   
      703 +        val nonKey = emptyIndices.first { it !in keys }       
      704  
      705 -        // Tap each key in turn -> hint clears                
      705 +        // 1. Value placement in any key cell.                
      706          for (key in keys) {
      707              var s = GameReducer.setPendingHint(s0, hint, keys)
      708              s = GameReducer.selectDigit(s, 1)
      709              s = GameReducer.tapCell(s, key / 9, key % 9)
      710 -            assertNull(s.pendingHint, "tapping key cell $key s
          -hould clear the pending hint")                                
      710 +            assertNull(s.pendingHint, "value in key cell $key 
          +should clear")                                                
      711          }
      712 -        // Tapping a non-key cell preserves it.               
      712 +        // 2. Value placement in a non-key cell still clears. 
      713          var s = GameReducer.setPendingHint(s0, hint, keys)
      714          s = GameReducer.selectDigit(s, 1)
      715 -        s = GameReducer.tapCell(s, otherEmpty / 9, otherEmpty 
          -% 9)                                                          
      716 -        assertNotNull(s.pendingHint, "tapping a non-key cell m
          -ust not clear the pending hint")                              
      717 -    }                                                         
      718 -                                                              
      719 -    @Test                                                     
      720 -    fun `placing a value in a different cell does not clear a 
          -pending hint`() {                                             
      721 -        val s0 = initial()                                    
      722 -        val grid = IntArray(81) { i -> s0.cells[i].value ?: 0 
          -}                                                             
      723 -        val hint = HintEngine.find(grid) as HintEngine.NakedSi
          -ngle                                                          
      724 -        val tr = hint.targetCell / 9                          
      725 -        val tc = hint.targetCell % 9                          
      726 -        // Pick any non-given cell that is not the target.    
      727 -        var otherIdx = -1                                     
      728 -        for (i in 0 until 81) {                               
      729 -            if (i == hint.targetCell) continue                
      730 -            if (!s0.cells[i].given && s0.cells[i].value == nul
          -l) { otherIdx = i; break }                                    
      731 -        }                                                     
      732 -        check(otherIdx != -1)                                 
      733 -        var s = GameReducer.setPendingHint(s0, hint, setOf(hin
          -t.targetCell))                                                
      715 +        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9)    
      716 +        assertNull(s.pendingHint, "value in a non-key cell mus
          +t also clear")                                                
      717 +        // 3. Pencil-mark toggle clears.                      
      718 +        s = GameReducer.setPendingHint(s0, hint, keys)        
      719 +        s = GameReducer.togglePencil(s)                       
      720          s = GameReducer.selectDigit(s, 1)
      735 -        s = GameReducer.tapCell(s, otherIdx / 9, otherIdx % 9)
      736 -        assertNotNull(s.pendingHint)                          
      737 -        // Sanity: we didn't accidentally touch the target.   
      738 -        assertNull(s.cellAt(tr, tc).value)                    
      721 +        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9)    
      722 +        assertNull(s.pendingHint, "pencil-mark toggle must cle
          +ar the pending hint")                                         
      723 +        // 4. Double-tap clear on a filled cell clears too.   
      724 +        s = initial().let { GameReducer.selectDigit(it, 4) }  
      725 +        s = GameReducer.tapCell(s, nonKey / 9, nonKey % 9) // 
          +fill it                                                       
      726 +        s = GameReducer.setPendingHint(s, hint, keys)         
      727 +        s = GameReducer.doubleTapCell(s, nonKey / 9, nonKey % 
          +9)                                                            
      728 +        assertNull(s.pendingHint, "double-tap clear must clear
          + the pending hint")                                           
      729      }
      730  
      731      @Test

● Update the docs to reflect the new behavior:

● Update(/home/user/soduku-trainer/docs/testing/progressive-hint-tests.md)  ⎿  Added 8 lines, removed 17 lines
      10  
      11  In addition, the pending hint clears automatically when:
      12  
      13 -- the player places a value in **any** of the highlighted key c
         -ells                                                           
      14 -  (correctly or incorrectly), or                               
      13 +- the player places **any** value, toggles **any** pencil mark,
         + or                                                            
      14 +  double-taps to clear **any** cell, or                        
      15  - the 60-second timer expires.
      16  
      17  Candidate Sweep through the Hint button does **not** use this f
          low
     ...
      34    - `selectDigit(wrong)` + `tapCell(target)` — the cell now has
           the
      35      wrong value AND `pendingHint` is null.
      36  
      37 -- **`placing a value in a different cell does not clear a pendi
         -ng                                                             
      38 -  hint`**                                                      
      39 -  - Set pending; tap a different non-given empty cell.         
      40 -  - `pendingHint` still present; the target cell still empty.  
      41 -                                                               
      42 -- **`pending hint covers multi-cell patterns`** *(new, see belo
         -w)*                                                            
      43 -  - Set pending with `keyCells = {a, b, c}` for a Naked Triple-
         -shaped                                                         
      44 -    hint.                                                      
      45 -  - Tap value into `a` — hint clears.                          
      46 -  - Re-set pending; tap value into `c` — hint clears.          
      47 -  - Re-set pending; tap value into a cell **not** in `keyCells`
         - —                                                             
      48 -    hint persists.                                             
      49 -                                                               
      50 -This last one guards against the targetCell-only check that the
      51 -single-cell version of `PendingHint` used to have.             
      37 +- **`any cell change clears the pending hint`**                
      38 +  - Set pending with multi-cell `keyCells`.                    
      39 +  - Tapping a value into a key cell clears it.                 
      40 +  - Tapping a value into a non-key cell also clears it.        
      41 +  - Toggling a pencil mark clears it.                          
      42 +  - Double-tapping a filled cell to clear it also clears the hi
         +nt.                                                            

● Now improve the fireworks. Larger bursts, more dramatic motion, brighter
  color, sparkly trails:

● Write(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrain
       er/ui/game/CelebrationOverlay.kt)
  ⎿  Added 184 lines, removed 32 lines
       8  import androidx.compose.runtime.remember
       9  import androidx.compose.ui.Modifier
      10  import androidx.compose.ui.geometry.Offset
      11 +import androidx.compose.ui.graphics.BlendMode                  
      12  import androidx.compose.ui.graphics.Color
      13  import kotlinx.coroutines.delay
      14  import kotlinx.coroutines.isActive
      15 +import kotlin.math.PI                                          
      16  import kotlin.math.cos
      17  import kotlin.math.sin
      18  import kotlin.random.Random
      19  
      20  private val FIREWORK_COLORS = listOf(
      19 -    Color(0xFFFFD700),                                         
      20 -    Color(0xFFFF4444),                                         
      21 -    Color(0xFF44FF44),                                         
      22 -    Color(0xFF4488FF),                                         
      23 -    Color(0xFFFF44FF),                                         
      24 -    Color(0xFFFFFFFF),                                         
      25 -    Color(0xFFFF8800),                                         
      26 -    Color(0xFF44FFFF),                                         
      21 +    Color(0xFFFFD700), // gold                                 
      22 +    Color(0xFFFF3D6E), // hot pink                             
      23 +    Color(0xFF34E89E), // green                                
      24 +    Color(0xFF4DA6FF), // sky blue                             
      25 +    Color(0xFFB388FF), // lavender                             
      26 +    Color(0xFFFFFFFF), // white                                
      27 +    Color(0xFFFF8C42), // orange                               
      28 +    Color(0xFF18FFFF), // cyan                                 
      29  )
      30  
      31 +private enum class Shape { Ring, Burst, Chrysanthemum, Willow }
      32 +                                                               
      33  private data class Particle(
      34      var x: Float,
      35      var y: Float,
     ...
      39      var life: Float = 1f,
      40      val decay: Float,
      41      val size: Float,
      42 +    val trail: ArrayDeque<Offset> = ArrayDeque(),              
      43 +    val sparkle: Boolean,                                      
      44  )
      45  
      46  /**
      47   * Fireworks particle overlay. [speedMultiplier] scales particl
          e velocity
      48   * and [durationMs] controls how long the animation runs (burst
          s are
      49   * spread evenly across the window).
      50 + *                                                             
      51 + * Bursts pick a random shape (ring / burst / chrysanthemum / w
         +illow),                                                        
      52 + * carry short fading trails behind each particle, and overdraw
         + with                                                          
      53 + * Plus blend mode so overlapping particles glow hot. Bursts si
         +ze scales                                                      
      54 + * by a uniform random factor so the screen isn't full of same-
         +size                                                           
      55 + * bursts.                                                     
      56   */
      57  @Composable
      58  fun CelebrationOverlay(
     ...
      65      LaunchedEffect(Unit) {
      66          val rng = Random.Default
      67          val frameDelay = 16L
      56 -        val burstInterval = 1500L                              
      68 +        val burstInterval = 900L                               
      69          val maxBursts = (durationMs / burstInterval).toInt().co
          erceAtLeast(4)
      70          var burstCount = 0
      71          var elapsed = 0L
      72  
      73          while (isActive && elapsed < durationMs) {
      62 -            if (burstCount < maxBursts && (burstCount == 0 || e
         -lapsed >= burstCount * burstInterval)) {                       
      63 -                val cx = 0.1f + rng.nextFloat() * 0.8f         
      64 -                val cy = 0.1f + rng.nextFloat() * 0.5f         
      65 -                val burstColor = FIREWORK_COLORS[rng.nextInt(FI
         -REWORK_COLORS.size)]                                           
      66 -                val count = 50 + rng.nextInt(40)               
      67 -                for (i in 0 until count) {                     
      68 -                    val angle = rng.nextFloat() * 2f * Math.PI.
         -toFloat()                                                      
      69 -                    val baseSpeed = 0.002f + rng.nextFloat() * 
         -0.006f                                                         
      70 -                    particles += Particle(                     
      71 -                        x = cx,                                
      72 -                        y = cy,                                
      73 -                        vx = cos(angle) * baseSpeed * speedMult
         -iplier,                                                        
      74 -                        vy = sin(angle) * baseSpeed * speedMult
         -iplier,                                                        
      75 -                        color = if (rng.nextFloat() < 0.3f) Col
         -or.White else burstColor,                                      
      76 -                        decay = 0.004f + rng.nextFloat() * 0.00
         -8f,                                                            
      77 -                        size = 3f + rng.nextFloat() * 5f,      
      78 -                    )                                          
      79 -                }                                              
      74 +            val due = burstCount == 0 || elapsed >= burstCount 
         +* burstInterval                                                
      75 +            if (burstCount < maxBursts && due) {               
      76 +                spawnBurst(particles, rng, speedMultiplier)    
      77                  burstCount++
      78 +                // Occasionally double up so it doesn't feel me
         +tronomic.                                                      
      79 +                if (rng.nextFloat() < 0.35f && burstCount < max
         +Bursts) {                                                      
      80 +                    spawnBurst(particles, rng, speedMultiplier)
      81 +                    burstCount++                               
      82 +                }                                              
      83              }
      84  
      85              delay(frameDelay)
     ...
       88              val iter = particles.listIterator()
       89              while (iter.hasNext()) {
       90                  val p = iter.next()
       91 +                // Tuck the previous position into the trail b
          +efore moving.                                                 
       92 +                p.trail.addFirst(Offset(p.x, p.y))            
       93 +                if (p.trail.size > MAX_TRAIL) p.trail.removeLa
          +st()                                                          
       94                  p.x += p.vx
       95                  p.y += p.vy
       91 -                p.vy += 0.00008f * speedMultiplier            
       92 -                p.vx *= 0.99f                                 
       93 -                p.vy *= 0.99f                                 
       96 +                p.vy += 0.00012f * speedMultiplier  // gravity
       97 +                p.vx *= 0.985f                                
       98 +                p.vy *= 0.985f                                
       99                  p.life -= p.decay
      100                  if (p.life <= 0f) iter.remove()
      101              }
     ...
      103      }
      104  
      105      Canvas(modifier = modifier.fillMaxSize()) {
      106 +        val w = size.width                                    
      107 +        val h = size.height                                   
      108          for (p in particles) {
      109 +            val alpha = p.life.coerceIn(0f, 1f)               
      110 +            // Trail: progressively smaller + dimmer dots behi
          +nd the head.                                                  
      111 +            var step = 0                                      
      112 +            for (t in p.trail) {                              
      113 +                step++                                        
      114 +                val trailAlpha = alpha * (1f - step.toFloat() 
          +/ MAX_TRAIL) * 0.55f                                          
      115 +                if (trailAlpha <= 0.02f) continue             
      116 +                drawCircle(                                   
      117 +                    color = p.color.copy(alpha = trailAlpha), 
      118 +                    radius = (p.size * p.life) * (1f - step.to
          +Float() / (MAX_TRAIL + 2)),                                   
      119 +                    center = Offset(t.x * w, t.y * h),        
      120 +                    blendMode = BlendMode.Plus,               
      121 +                )                                             
      122 +            }                                                 
      123 +            // Head: bright glow surrounded by an even brighte
          +r core.                                                       
      124 +            val cx = p.x * w                                  
      125 +            val cy = p.y * h                                  
      126              drawCircle(
      103 -                color = p.color.copy(alpha = p.life.coerceIn(0
          -f, 1f)),                                                      
      127 +                color = p.color.copy(alpha = alpha * 0.45f),  
      128 +                radius = p.size * p.life * 2.2f,              
      129 +                center = Offset(cx, cy),                      
      130 +                blendMode = BlendMode.Plus,                   
      131 +            )                                                 
      132 +            drawCircle(                                       
      133 +                color = p.color.copy(alpha = alpha),          
      134                  radius = p.size * p.life,
      105 -                center = Offset(p.x * size.width, p.y * size.h
          -eight),                                                       
      135 +                center = Offset(cx, cy),                      
      136 +                blendMode = BlendMode.Plus,                   
      137              )
      138 +            if (p.sparkle) {                                  
      139 +                drawCircle(                                   
      140 +                    color = Color.White.copy(alpha = alpha * 0
          +.9f),                                                         
      141 +                    radius = (p.size * p.life) * 0.45f,       
      142 +                    center = Offset(cx, cy),                  
      143 +                    blendMode = BlendMode.Plus,               
      144 +                )                                             
      145 +            }                                                 
      146          }
      147      }
      148  }
      149 +                                                              
      150 +private const val MAX_TRAIL = 8                               
      151 +                                                              
      152 +private fun spawnBurst(                                       
      153 +    particles: MutableList<Particle>,                         
      154 +    rng: Random,                                              
      155 +    speedMultiplier: Float,                                   
      156 +) {                                                           
      157 +    val cx = 0.1f + rng.nextFloat() * 0.8f                    
      158 +    val cy = 0.08f + rng.nextFloat() * 0.55f                  
      159 +    val burstColor = FIREWORK_COLORS[rng.nextInt(FIREWORK_COLO
          +RS.size)]                                                     
      160 +    val secondaryColor = FIREWORK_COLORS[rng.nextInt(FIREWORK_
          +COLORS.size)]                                                 
      161 +    // Vary burst scale so the show feels less uniform.       
      162 +    val sizeFactor = 0.7f + rng.nextFloat() * 1.1f            
      163 +    val count = (80 + rng.nextInt(80) * sizeFactor).toInt()   
      164 +    val baseSpeedFloor = 0.0035f * sizeFactor                 
      165 +    val baseSpeedRange = 0.0085f * sizeFactor                 
      166 +                                                              
      167 +    when (Shape.entries[rng.nextInt(Shape.entries.size)]) {   
      168 +        Shape.Ring -> {                                       
      169 +            // Even ring at a single radius — the classic "O".
      170 +            val ringSpeed = baseSpeedFloor + baseSpeedRange * 
          +0.7f                                                          
      171 +            for (i in 0 until count) {                        
      172 +                val angle = (i.toFloat() / count) * 2f * PI.to
          +Float()                                                       
      173 +                particles += newParticle(                     
      174 +                    cx, cy,                                   
      175 +                    cos(angle) * ringSpeed * speedMultiplier, 
      176 +                    sin(angle) * ringSpeed * speedMultiplier, 
      177 +                    pickColor(rng, burstColor, secondaryColor)
          +,                                                             
      178 +                    decay = 0.0035f + rng.nextFloat() * 0.004f
          +,                                                             
      179 +                    size = (4.5f + rng.nextFloat() * 4f) * siz
          +eFactor,                                                      
      180 +                    sparkle = rng.nextFloat() < 0.25f,        
      181 +                )                                             
      182 +            }                                                 
      183 +        }                                                     
      184 +        Shape.Burst -> {                                      
      185 +            // Random angle + speed — chaotic spray.          
      186 +            for (i in 0 until count) {                        
      187 +                val angle = rng.nextFloat() * 2f * PI.toFloat(
          +)                                                             
      188 +                val speed = baseSpeedFloor + rng.nextFloat() *
          + baseSpeedRange                                               
      189 +                particles += newParticle(                     
      190 +                    cx, cy,                                   
      191 +                    cos(angle) * speed * speedMultiplier,     
      192 +                    sin(angle) * speed * speedMultiplier,     
      193 +                    pickColor(rng, burstColor, secondaryColor)
          +,                                                             
      194 +                    decay = 0.004f + rng.nextFloat() * 0.006f,
      195 +                    size = (4f + rng.nextFloat() * 5f) * sizeF
          +actor,                                                        
      196 +                    sparkle = rng.nextFloat() < 0.3f,         
      197 +                )                                             
      198 +            }                                                 
      199 +        }                                                     
      200 +        Shape.Chrysanthemum -> {                              
      201 +            // Dense layered ring — two speeds, brighter core.
      202 +            for (i in 0 until count) {                        
      203 +                val angle = (i.toFloat() / count) * 2f * PI.to
          +Float() +                                                     
      204 +                    (rng.nextFloat() - 0.5f) * 0.08f          
      205 +                val outerLayer = i % 2 == 0                   
      206 +                val speed = if (outerLayer) {                 
      207 +                    baseSpeedFloor + baseSpeedRange * 0.8f    
      208 +                } else {                                      
      209 +                    baseSpeedFloor + baseSpeedRange * 0.45f   
      210 +                }                                             
      211 +                particles += newParticle(                     
      212 +                    cx, cy,                                   
      213 +                    cos(angle) * speed * speedMultiplier,     
      214 +                    sin(angle) * speed * speedMultiplier,     
      215 +                    pickColor(rng, burstColor, secondaryColor)
          +,                                                             
      216 +                    decay = 0.003f + rng.nextFloat() * 0.004f,
      217 +                    size = (4.5f + rng.nextFloat() * 5f) * siz
          +eFactor,                                                      
      218 +                    sparkle = rng.nextFloat() < 0.35f,        
      219 +                )                                             
      220 +            }                                                 
      221 +        }                                                     
      222 +        Shape.Willow -> {                                     
      223 +            // Heavy downward-trailing burst — short upward ar
          +c, long fall.                                                 
      224 +            for (i in 0 until count) {                        
      225 +                val angle = rng.nextFloat() * 2f * PI.toFloat(
          +)                                                             
      226 +                val speed = baseSpeedFloor * 0.7f + rng.nextFl
          +oat() * baseSpeedRange * 0.6f                                 
      227 +                particles += newParticle(                     
      228 +                    cx, cy,                                   
      229 +                    cos(angle) * speed * speedMultiplier,     
      230 +                    sin(angle) * speed * speedMultiplier - 0.0
          +015f * speedMultiplier,                                       
      231 +                    pickColor(rng, burstColor, secondaryColor)
          +,                                                             
      232 +                    decay = 0.0025f + rng.nextFloat() * 0.0035
          +f,                                                            
      233 +                    size = (4f + rng.nextFloat() * 4f) * sizeF
          +actor,                                                        
      234 +                    sparkle = rng.nextFloat() < 0.4f,         
      235 +                )                                             
      236 +            }                                                 
      237 +        }                                                     
      238 +    }                                                         
      239 +}                                                             
      240 +                                                              
      241 +private fun pickColor(rng: Random, primary: Color, secondary: 
          +Color): Color = when {                                        
      242 +    rng.nextFloat() < 0.18f -> Color.White                    
      243 +    rng.nextFloat() < 0.45f -> secondary                      
      244 +    else -> primary                                           
      245 +}                                                             
      246 +                                                              
      247 +private fun newParticle(                                      
      248 +    x: Float, y: Float,                                       
      249 +    vx: Float, vy: Float,                                     
      250 +    color: Color,                                             
      251 +    decay: Float,                                             
      252 +    size: Float,                                              
      253 +    sparkle: Boolean,                                         
      254 +): Particle = Particle(                                       
      255 +    x = x, y = y,                                             
      256 +    vx = vx, vy = vy,                                         
      257 +    color = color,                                            
      258 +    decay = decay,                                            
      259 +    size = size,                                              
      260 +    sparkle = sparkle,                                        
      261 +)                                                             

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:compileJava NO-SOURCE
     > Task :domain:classes UP-TO-DATE
     > Task :domain:jar
     … +7 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Broaden pending-hint clearing + revamp fireworks…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 25e96f6] Broaden pending-hint cle
     aring + revamp fireworks
      4 files changed, 221 insertions(+), 90 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed 3bcbd4f.

  Hint timeout: the 60s timer was already in place from earlier. What changed:
  pendingHint now clears on any board-changing player action — value placement
  (any cell, not just a key cell), pencil-mark toggle, double-tap clear. Rewrote
  any cell change clears the pending hint to lock that in and updated
  docs/testing/progressive-hint-tests.md.

  Fireworks rewrite:
  - Bigger: burst particle count went 50–90 → 80–160, multiplied by a random
  per-burst scale factor (0.7–1.8). Each particle's size and trail also scales
  with that factor.
  - Brighter: drawing uses BlendMode.Plus, so overlapping particles add color
  and hot regions blow out white. Every particle also has a 2.2× wider faint
  glow halo plus the bright core.
  - Trails: each particle keeps an 8-frame breadcrumb of fading dots behind it —
  sparklers/willow effect.
  - Sparkles: ~25–40% of particles get a small white sparkle dot on top.
  - More variety: four burst shapes pick randomly per burst — Ring (clean O),
  Burst (chaotic spray), Chrysanthemum (dense layered ring), Willow
  (downward-trailing droop). Two colors mix into each burst.
  - Faster cadence: burst interval 1500ms → 900ms with a 35% chance of
  double-bursting, so the sky stays full without feeling metronomic.

  firework_speed and firework_duration config still work the same way.

❯ Instead of showing the squares for a hint first, show the solve method. Then  
  show the squares                                                              
  The hint needs to disappear (both text, and highlighted squares), after 10    
  seconds, any move, or whichever comes first. A move would involve changing a  
  note, or placing a number.                                                    
                                                                                
  The autosolve should count all numbers placed for determining whether or not  
  it should be allowed, regardless of color. The autosolve should automatically 
  run once eligible.                                                            
                                                                                
  Can you make the fireworks thicker?                                           

● Several changes. Let me work through them.

  1. Flip the progressive hint: stage 1 shows the technique name, stage 2 adds 
  the cell highlights.

● Write(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokutr
       ainer/domain/PendingHint.kt)
  ⎿  Added 5 lines, removed 6 lines
       5   * figure the pattern out themselves with progressive disclosur
          e.
       6   *
       7   * Stage progression (driven by repeated Hint presses):
       8 - *  1. [showMethod] = false: [keyCells] are highlighted, nothin
         -g else.                                                        
       9 - *  2. [showMethod] = true:  the technique name is also reveale
         -d.                                                             
       8 + *  1. [showCells] = false: the technique name is revealed, not
         +hing else.                                                     
       9 + *  2. [showCells] = true:  the cells of the pattern are highli
         +ghted too.                                                     
      10   *  3. *next press* fires the existing animation and clears thi
          s.
      11   *
      12 - * Cleared automatically when the timer expires, when the playe
         -r places                                                       
      13 - * a value in any of [keyCells], or when the game phase changes
         -. For                                                          
      14 - * single-cell hints (Naked/Hidden Single) [keyCells] is a sing
         -leton.                                                         
      12 + * Cleared automatically when the timer expires, when the playe
         +r makes                                                        
      13 + * any board-changing action, or when the game phase changes.  
      14   */
      15  data class PendingHint(
      16      val hint: HintEngine.Hint,
      17      val keyCells: Set<Int>,
      18      val techniqueName: String,
      20 -    val showMethod: Boolean = false,                           
      19 +    val showCells: Boolean = false,                            
      20  )

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 6 lines, removed 6 lines
      213  
      214      // ---- Progressive-hint state ----
      215  
      216 -    /** Stage 1: key cells highlighted, technique name hidden.
          - */                                                           
      216 +    /** Stage 1: technique name revealed, cells not highlighte
          +d yet. */                                                     
      217      fun setPendingHint(state: GameState, hint: HintEngine.Hint
           , keyCells: Set<Int>): GameState =
      218          state.copy(pendingHint = PendingHint(
      219              hint = hint,
      220              keyCells = keyCells,
      221              techniqueName = hint.techniqueName,
      222 -            showMethod = false,                               
      222 +            showCells = false,                                
      223          ))
      224  
      225 -    /** Stage 2: reveal the technique name; no-op if already s
          -hown or unset. */                                             
      226 -    fun advancePendingHintToMethod(state: GameState): GameStat
          -e {                                                           
      225 +    /** Stage 2: also highlight the pattern's cells. No-op if 
          +already shown. */                                             
      226 +    fun advancePendingHintRevealCells(state: GameState): GameS
          +tate {                                                        
      227          val ph = state.pendingHint ?: return state
      228 -        if (ph.showMethod) return state                       
      229 -        return state.copy(pendingHint = ph.copy(showMethod = t
          -rue))                                                         
      228 +        if (ph.showCells) return state                        
      229 +        return state.copy(pendingHint = ph.copy(showCells = tr
          +ue))                                                          
      230      }
      231  
      232      fun clearPendingHint(state: GameState): GameState =

● Now thread the rename through the ViewModel, the HintLabel (always shows when
  pending), and the Board (gated on showCells):

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 2 lines, removed 2 lines
      149          // Stage 2 or 3: a pending hint already exists from a 
           prior press.
      150          val ph = s.pendingHint
      151          if (ph != null) {
      152 -            if (!ph.showMethod) {                             
      153 -                _state.value = GameReducer.advancePendingHintT
          -oMethod(s)                                                    
      152 +            if (!ph.showCells) {                              
      153 +                _state.value = GameReducer.advancePendingHintR
          +evealCells(s)                                                 
      154                  startPendingHintTimer()
      155              } else {
      156                  // Stage 3: clear the pending hint and run the
            animation.

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameScreen.kt)
  ⎿  Added 1 line, removed 1 line
       98          is HintUi.CandidateSweep -> "Candidate Sweep"
       99          is HintUi.Elimination -> ui.techniqueName
      100          HintUi.NoHintFlash -> "No hint available"
      101 -        null -> if (pending?.showMethod == true) pending.techn
          -iqueName else ""                                              
      101 +        null -> pending?.techniqueName.orEmpty()              
      102      }
      103      Box(
      104          modifier = Modifier

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/Board.kt)
  ⎿  Added 4 lines, removed 3 lines
      125                          }
      126                      }
      127  
      128 -                    // Progressive-hint key-cell highlight (dr
          -awn only when                                                 
      129 -                    // no active animation overlay is competin
          -g).                                                           
      128 +                    // Progressive-hint key-cell highlight (st
          +age 2 only,                                                   
      129 +                    // and only when no active animation overl
          +ay is                                                         
      130 +                    // competing for the same cells).         
      131                      val pending = state.pendingHint
      131 -                    if (hint == null && pending != null) {    
      132 +                    if (hint == null && pending != null && pen
          +ding.showCells) {                                             
      133                          for (idx in pending.keyCells) {
      134                              val r = idx / 9; val c = idx % 9
      135                              drawRect(

● Fix the existing reducer tests that still reference the old field:

  Searched for 1 pattern (ctrl+o to expand)

● Bash(sed -i 's/showMethod/showCells/g;
      s/advancePendingHintToMethod/advancePendingHintRevealCells/g'
      /home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/…)
  ⎿  /home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudoku
     trainer/domain/GameReducerTest.kt:660:        assertEquals(false, s.pe
     ndingHint?.showCells)
     … +12 lines (ctrl+o to expand)

● 2. Timer to 10 seconds:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12      val sweepCellMs: Long = 150,
      13 -    val pendingHintMs: Long = 60_000,                          
      13 +    val pendingHintMs: Long = 10_000,                          
      14      val showAgainMultiplier: Float = 3f,
      15      val showTestButton: Boolean = true,
      16  ) {

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
      39                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      40                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      41                  sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
          l() ?: 150,
      42 -                pendingHintMs = map["pending_hint_ms"]?.toLongO
         -rNull() ?: 60_000,                                             
      42 +                pendingHintMs = map["pending_hint_ms"]?.toLongO
         +rNull() ?: 10_000,                                             
      43                  showAgainMultiplier = map["show_again_multiplie
          r"]?.toFloatOrNull() ?: 3f,
      44                  showTestButton = map["show_test_button"]?.toBoo
          leanStrictOrNull() ?: true,
      45              )

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 6 lines, removed 5 lines
      30  #                ticks faster than hint_step_ms.
      31  sweep_cell_ms: 150
      32  
      33 -# pending_hint_ms: How long the progressive-hint highlight (and
         - the                                                           
      34 -#                  revealed technique name on a second press) s
         -tays on                                                        
      35 -#                  screen before fading. Reset by each subseque
         -nt press                                                       
      36 -#                  that advances the stage.                    
      37 -pending_hint_ms: 60000                                         
      33 +# pending_hint_ms: How long a pending hint (technique name and 
         +any                                                            
      34 +#                  revealed cell highlights) stays on screen be
         +fore                                                           
      35 +#                  fading. Also reset by each subsequent press 
         +that                                                           
      36 +#                  advances the stage. Any board-changing playe
         +r action                                                       
      37 +#                  clears it immediately regardless of the time
         +r.                                                             
      38 +pending_hint_ms: 10000                                         
      39  
      40  # show_again_multiplier: How much slower the Show Again button 
          replays
      41  #                        the last hint compared to the normal a
          nimation

● 3 & 4. Autosolve counts every placed value (regardless of correctness), runs 
  automatically, and fixes wrong cells.

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameState.kt)
  ⎿  Added 3 lines, removed 2 lines
      34  
      35      /**
      36       * Check if the puzzle is ready for auto-solve: at most [so
          lveCriteria]
      37 -     * empty cells remain AND every filled cell is correct.    
      37 +     * empty cells remain. Correctness of the placed values doe
         +sn't matter                                                    
      38 +     * — auto-solve will overwrite any wrong entries with the s
         +olution.                                                       
      39       */
      40      fun isMostlySolved(solveCriteria: Int): Boolean =
      40 -        !hasIncorrectEntry && emptyCellCount <= solveCriteria  
      41 +        emptyCellCount <= solveCriteria                        
      42  
      43      fun placedCountOf(digit: Int): Int {
      44          require(digit in 1..9)

● Make applySolve and solveSingleCell fix incorrect cells, not just empty ones:
  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 7 lines, removed 2 lines
      668          )
      669  
      670      fun solveSingleCell(state: GameState): GameState {
      671 -        val idx = state.cells.indexOfFirst { !it.given && it.v
          -alue == null }                                                
      671 +        val idx = state.cells.indexOfFirst { c -> needsSolve(c
          +, state.puzzle) }                                             
      672          if (idx == -1) return state
      673          val cell = state.cells[idx]
      674          val newCell = cell.copy(
     ...
      681  
      682      fun applySolve(state: GameState): GameState {
      683          val newCells = state.cells.map { c ->
      684 -            if (c.isEmpty) {                                  
      684 +            if (needsSolve(c, state.puzzle)) {                
      685                  c.copy(
      686                      value = state.puzzle.solutionAt(c.row, c.c
           ol),
      687                      valueColor = CellColor.Solve,
     ...
      699          )
      700      }
      701  
      702 +    /** Non-given cell that is empty *or* holds an incorrect v
          +alue. */                                                      
      703 +    private fun needsSolve(cell: Cell, puzzle: Puzzle): Boolea
          +n =                                                           
      704 +        !cell.given &&                                        
      705 +            (cell.value == null || cell.value != puzzle.soluti
          +onAt(cell.row, cell.col))                                     
      706 +                                                              
      707      // ---- Helpers ----
      708  
      709      private fun GameState.replaceCell(newCell: Cell): GameStat
           e {

● Update the autosolve loop to fix the new behavior — exit when there's nothing
  left to solve, not just when empties hit zero:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 11 lines, removed 2 lines
       80  
       81      val gameConfig: GameConfig get() = config
       82  
       83 +    @Volatile private var autoSolveStarted: Boolean = false   
       84 +                                                              
       85      fun onSolve() {
       86 +        if (autoSolveStarted) return                          
       87 +        autoSolveStarted = true                               
       88          _state.value = GameReducer.beginAutoSolve(_state.value
           )
       89          viewModelScope.launch {
       86 -            while (_state.value.emptyCellCount > 0) {         
       90 +            // Loop until solveSingleCell reports no progress:
          + each call                                                    
       91 +            // fixes one empty *or* incorrect non-given cell. 
       92 +            while (true) {                                    
       93                  delay(config.solveSpeed)
       88 -                _state.value = GameReducer.solveSingleCell(_st
          -ate.value)                                                    
       94 +                val before = _state.value                     
       95 +                val after = GameReducer.solveSingleCell(before
          +)                                                             
       96 +                if (after === before) break                   
       97 +                _state.value = after                          
       98              }
       99              _state.value = _state.value.copy(phase = GameState
           .Phase.Celebrating)
      100              delay(config.fireworkDuration)

● Now auto-trigger when isMostlySolved becomes true. Add an init block that
  watches state:

  Read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 15 lines
      42          setState = { _state.value = it },
      43      )
      44  
      45 +    init {                                                     
      46 +        // Auto-trigger solve when the board has few enough emp
         +ty cells                                                       
      47 +        // — fires once, regardless of correctness of placed va
         +lues.                                                          
      48 +        viewModelScope.launch {                                
      49 +            _state.collect { s ->                              
      50 +                if (!autoSolveStarted &&                       
      51 +                    s.phase == GameState.Phase.Playing &&      
      52 +                    s.isMostlySolved(config.solveCriteria)     
      53 +                ) {                                            
      54 +                    onSolve()                                  
      55 +                }                                              
      56 +            }                                                  
      57 +        }                                                      
      58 +    }                                                          
      59 +                                                               
      60      /** Whether the puzzle qualifies for auto-solve (driven by 
          config.yaml). */
      61      val isReadyToSolve: Boolean
      62          get() = _state.value.isMostlySolved(config.solveCriteri
          a)

● Existing test asserted the old semantics — update it:

  Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 18 lines, removed 8 lines
      215      }
      216  
      217      @Test
      218 -    fun `mostly solved requires threshold AND no incorrect ent
          -ries`() {                                                     
      218 +    fun `mostly solved counts any placed value regardless of c
          +orrectness`() {                                               
      219          var s = initial()
      220          // Fill all non-given cells correctly using the soluti
           on as the active digit.
      221          // selectDigit toggles off when invoked with the curre
           ntly-active digit,
     ...
      230          }
      231          assertEquals(81, s.correctCount)
      232          assertTrue(s.isMostlySolved(10))
      233 -        // Introduce one incorrect entry: pick a non-given cel
          -l, clear it,                                                  
      234 -        // and place a wrong value.                           
      235 -        var wrongTarget = -1                                  
      236 -        for (i in 0 until 81) {                               
      237 -            if (givens[i] == 0) { wrongTarget = i; break }    
      238 -        }                                                     
      233 +        // Replace one correct entry with a wrong digit. Mostl
          +y-solved                                                      
      234 +        // should *still* be true — auto-solve will overwrite 
          +the wrong                                                     
      235 +        // value when it runs.                                
      236 +        val wrongTarget = (0 until 81).first { givens[it] == 0
          + }                                                            
      237          val wr = wrongTarget / 9
      238          val wc = wrongTarget % 9
      239          s = GameReducer.doubleTapCell(s, wr, wc)
     ...
      242          s = GameReducer.selectDigit(s, wrong)
      243          s = GameReducer.tapCell(s, wr, wc)
      244          assertTrue(s.hasIncorrectEntry)
      247 -        assertFalse(s.isMostlySolved(10), "any incorrect entry
          - must block mostly-solved")                                   
      245 +        assertTrue(s.isMostlySolved(10), "wrong entries no lon
          +ger block mostly-solved")                                     
      246      }
      247  
      248      @Test
      249 +    fun `applySolve overwrites incorrect non-given cells with 
          +the solution`() {                                             
      250 +        var s = initial()                                     
      251 +        // Place a wrong digit at (0,2): solution = 4, place 7
          +.                                                             
      252 +        s = GameReducer.selectDigit(s, 7)                     
      253 +        s = GameReducer.tapCell(s, 0, 2)                      
      254 +        assertEquals(7, s.cellAt(0, 2).value)                 
      255 +        s = GameReducer.applySolve(s)                         
      256 +        assertEquals(solution[2], s.cellAt(0, 2).value)       
      257 +        assertEquals(CellColor.Solve, s.cellAt(0, 2).valueColo
          +r)                                                            
      258 +    }                                                         
      259 +                                                              
      260 +    @Test                                                     
      261      fun `applySolve fills empty cells in Solve color and trans
           itions phase`() {
      262          var s = initial()
      263          s = GameReducer.selectDigit(s, 4)

● 5. Thicker fireworks — bump particle sizes and trail radius:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 19 lines, removed 9 lines
      107          val h = size.height
      108          for (p in particles) {
      109              val alpha = p.life.coerceIn(0f, 1f)
      110 -            // Trail: progressively smaller + dimmer dots behi
          -nd the head.                                                  
      110 +            // Trail: chunkier than the previous version — wid
          +er radius                                                     
      111 +            // multiplier and slower-fading alpha so the strea
          +k reads.                                                      
      112              var step = 0
      113              for (t in p.trail) {
      114                  step++
      114 -                val trailAlpha = alpha * (1f - step.toFloat() 
          -/ MAX_TRAIL) * 0.55f                                          
      115 +                val trailAlpha = alpha * (1f - step.toFloat() 
          +/ MAX_TRAIL) * 0.7f                                           
      116                  if (trailAlpha <= 0.02f) continue
      117                  drawCircle(
      118                      color = p.color.copy(alpha = trailAlpha),
      118 -                    radius = (p.size * p.life) * (1f - step.to
          -Float() / (MAX_TRAIL + 2)),                                   
      119 +                    radius = (p.size * p.life) * 1.4f *       
      120 +                        (1f - step.toFloat() / (MAX_TRAIL + 2)
          +),                                                            
      121                      center = Offset(t.x * w, t.y * h),
      122                      blendMode = BlendMode.Plus,
      123                  )
      124              }
      123 -            // Head: bright glow surrounded by an even brighte
          -r core.                                                       
      125 +            // Head: thick three-layer stack — wide outer glow
          +, medium                                                      
      126 +            // halo, bright core — so each particle looks like
          + a fat                                                        
      127 +            // bead instead of a thin dot.                    
      128              val cx = p.x * w
      129              val cy = p.y * h
      130              drawCircle(
      127 -                color = p.color.copy(alpha = alpha * 0.45f),  
      128 -                radius = p.size * p.life * 2.2f,              
      131 +                color = p.color.copy(alpha = alpha * 0.3f),   
      132 +                radius = p.size * p.life * 3.4f,              
      133                  center = Offset(cx, cy),
      134                  blendMode = BlendMode.Plus,
      135              )
      136              drawCircle(
      137 +                color = p.color.copy(alpha = alpha * 0.55f),  
      138 +                radius = p.size * p.life * 2.0f,              
      139 +                center = Offset(cx, cy),                      
      140 +                blendMode = BlendMode.Plus,                   
      141 +            )                                                 
      142 +            drawCircle(                                       
      143                  color = p.color.copy(alpha = alpha),
      134 -                radius = p.size * p.life,                     
      144 +                radius = p.size * p.life * 1.35f,             
      145                  center = Offset(cx, cy),
      146                  blendMode = BlendMode.Plus,
      147              )
      148              if (p.sparkle) {
      149                  drawCircle(
      140 -                    color = Color.White.copy(alpha = alpha * 0
          -.9f),                                                         
      141 -                    radius = (p.size * p.life) * 0.45f,       
      150 +                    color = Color.White.copy(alpha = alpha * 0
          +.95f),                                                        
      151 +                    radius = (p.size * p.life) * 0.6f,        
      152                      center = Offset(cx, cy),
      153                      blendMode = BlendMode.Plus,
      154                  )

● Also bump base particle sizes so the bursts feel chunkier:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 1 line, removed 1 line
      186                      sin(angle) * ringSpeed * speedMultiplier,
      187                      pickColor(rng, burstColor, secondaryColor)
           ,
      188                      decay = 0.0035f + rng.nextFloat() * 0.004f
           ,
      189 -                    size = (4.5f + rng.nextFloat() * 4f) * siz
          -eFactor,                                                      
      189 +                    size = (7f + rng.nextFloat() * 5f) * sizeF
          +actor,                                                        
      190                      sparkle = rng.nextFloat() < 0.25f,
      191                  )
      192              }

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 1 line, removed 1 line
      202                      sin(angle) * speed * speedMultiplier,
      203                      pickColor(rng, burstColor, secondaryColor)
           ,
      204                      decay = 0.004f + rng.nextFloat() * 0.006f,
      205 -                    size = (4f + rng.nextFloat() * 5f) * sizeF
          -actor,                                                        
      205 +                    size = (6.5f + rng.nextFloat() * 6f) * siz
          +eFactor,                                                      
      206                      sparkle = rng.nextFloat() < 0.3f,
      207                  )
      208              }

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 1 line, removed 1 line
      240                      sin(angle) * speed * speedMultiplier - 0.0
           015f * speedMultiplier,
      241                      pickColor(rng, burstColor, secondaryColor)
           ,
      242                      decay = 0.0025f + rng.nextFloat() * 0.0035
           f,
      243 -                    size = (4f + rng.nextFloat() * 4f) * sizeF
          -actor,                                                        
      243 +                    size = (6f + rng.nextFloat() * 5f) * sizeF
          +actor,                                                        
      244                      sparkle = rng.nextFloat() < 0.4f,
      245                  )
      246              }

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -10)
  ⎿  > Task :domain:jar
     > Task :domain:compileTestKotlin
     > Task :domain:compileTestJava NO-SOURCE
     … +13 lines (ctrl+o to expand)

● All tests pass. Commit and push:

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Flip hint disclosure, shorten timer, auto-solve, thicker fireworks…)
  ⎿  [claude/add-sudoku-hint-button-L1spT e82787a] Flip hint disclosure, sh
     orten timer, auto-solve, thicker fireworks
      11 files changed, 108 insertions(+), 57 deletions(-)
     … +5 lines (ctrl+o to expand)

● Bash(git push 2>&1 | tail -3)
  ⎿  To have this happen automatically for branches without a tracking
     upstream, see 'push.autoSetupRemote' in 'git help config'.

● Bash(git push -u origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -3)
  ⎿  To http://127.0.0.1:41729/git/dividetask/soduku-trainer
        16c12d2..e82787a  claude/add-sudoku-hint-button-L1spT -> claude/add
     -sudoku-hint-button-L1spT
     … +2 lines (ctrl+o to expand)

● Pushed e82787a.

  Hint flow reversed: first press reveals the technique name (no highlight).
  Second press also highlights the pattern cells. Third press runs the
  animation. PendingHint.showMethod → showCells; the HintLabel shows the
  technique whenever a pending hint exists.

  Timer: pending_hint_ms default 60000 → 10000. Any board-changing action
  (value, pencil toggle, double-tap clear) still clears it instantly; updated
  config.yaml comment to reflect both paths.

  Auto-solve:
  - isMostlySolved now ignores correctness — any value placed in a non-given
  cell counts toward the threshold. The old "any incorrect entry blocks
  auto-solve" rule is gone (and the test renamed/updated accordingly).
  - solveSingleCell and applySolve now treat empty or incorrect non-given cells
  as needing solving, so wrong values get overwritten with the solution.
  - GameViewModel.init collects the state flow and calls onSolve() the moment
  the threshold is crossed. onSolve() has an autoSolveStarted guard so the
  StateFlow collector firing repeatedly during the solve animation can't
  re-enter.
  - The solve loop now exits when solveSingleCell reports no progress (returns
  the same state instance) instead of when emptyCellCount hits 0 — important
  because wrong-cell fixes don't change the empty count.

  Fireworks thicker: base particle sizes bumped from 4–5px → 6–7px across all
  four burst shapes; head now draws a three-layer stack (wide 3.4× glow + 2×
  halo + 1.35× bright core); trails widened to 1.4× of head radius with 0.7
  alpha instead of 0.55. Each spark now reads as a fat bead rather than a thin
  dot.

❯ Wait wait wait.                                                               
  "isMostlySolved now ignores correctness — any value placed in a non-given     
  cell counts toward the threshold. The old "any incorrect entry blocks         
  auto-solve" rule is gone (and the test renamed/updated accordingly)."         
  I didn't say ignore correctness. I said count cells of different colors as    
  cell solutions when determining correctness and completeness                  

● You're right, I misread. Reverting to keep the !hasIncorrectEntry check — the
  count was already color-agnostic, so nothing else needed changing on that
  front.
● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameState.kt)
  ⎿  Added 3 lines, removed 3 lines
      34  
      35      /**
      36       * Check if the puzzle is ready for auto-solve: at most [so
          lveCriteria]
      37 -     * empty cells remain. Correctness of the placed values doe
         -sn't matter                                                    
      38 -     * — auto-solve will overwrite any wrong entries with the s
         -olution.                                                       
      37 +     * empty cells remain AND every filled cell is correct. Cel
         +l color                                                        
      38 +     * doesn't matter — any placed value (Guess, Solve, etc.) c
         +ounts.                                                         
      39       */
      40      fun isMostlySolved(solveCriteria: Int): Boolean =
      41 -        emptyCellCount <= solveCriteria                        
      41 +        !hasIncorrectEntry && emptyCellCount <= solveCriteria  
      42  
      43      fun placedCountOf(digit: Int): Int {
      44          require(digit in 1..9)

● Revert solveSingleCell / applySolve to only touch empty cells, since
  correctness is now enforced:

● Update(/home/user/soduku-trainer/domain/src/main/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducer.kt)
  ⎿  Added 2 lines, removed 7 lines
      668          )
      669  
      670      fun solveSingleCell(state: GameState): GameState {
      671 -        val idx = state.cells.indexOfFirst { c -> needsSolve(c
          -, state.puzzle) }                                             
      671 +        val idx = state.cells.indexOfFirst { !it.given && it.v
          +alue == null }                                                
      672          if (idx == -1) return state
      673          val cell = state.cells[idx]
      674          val newCell = cell.copy(
     ...
      681  
      682      fun applySolve(state: GameState): GameState {
      683          val newCells = state.cells.map { c ->
      684 -            if (needsSolve(c, state.puzzle)) {                
      684 +            if (c.isEmpty) {                                  
      685                  c.copy(
      686                      value = state.puzzle.solutionAt(c.row, c.c
           ol),
      687                      valueColor = CellColor.Solve,
     ...
      699          )
      700      }
      701  
      702 -    /** Non-given cell that is empty *or* holds an incorrect v
          -alue. */                                                      
      703 -    private fun needsSolve(cell: Cell, puzzle: Puzzle): Boolea
          -n =                                                           
      704 -        !cell.given &&                                        
      705 -            (cell.value == null || cell.value != puzzle.soluti
          -onAt(cell.row, cell.col))                                     
      706 -                                                              
      702      // ---- Helpers ----
      703  
      704      private fun GameState.replaceCell(newCell: Cell): GameStat
           e {

● Revert the onSolve loop to the simpler emptyCellCount > 0 form:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/GameViewModel.kt)
  ⎿  Added 2 lines, removed 7 lines
      102          autoSolveStarted = true
      103          _state.value = GameReducer.beginAutoSolve(_state.value
           )
      104          viewModelScope.launch {
      105 -            // Loop until solveSingleCell reports no progress:
          - each call                                                    
      106 -            // fixes one empty *or* incorrect non-given cell. 
      107 -            while (true) {                                    
      105 +            while (_state.value.emptyCellCount > 0) {         
      106                  delay(config.solveSpeed)
      109 -                val before = _state.value                     
      110 -                val after = GameReducer.solveSingleCell(before
          -)                                                             
      111 -                if (after === before) break                   
      112 -                _state.value = after                          
      107 +                _state.value = GameReducer.solveSingleCell(_st
          +ate.value)                                                    
      108              }
      109              _state.value = _state.value.copy(phase = GameState
           .Phase.Celebrating)
      110              delay(config.fireworkDuration)

● Revert the test edits:

● Update(/home/user/soduku-trainer/domain/src/test/kotlin/com/dividetask/sudokut
        rainer/domain/GameReducerTest.kt)
  ⎿  Added 4 lines, removed 17 lines
      215      }
      216  
      217      @Test
      218 -    fun `mostly solved counts any placed value regardless of c
          -orrectness`() {                                               
      218 +    fun `mostly solved requires threshold AND no incorrect ent
          +ries`() {                                                     
      219          var s = initial()
      220          // Fill all non-given cells correctly using the soluti
           on as the active digit.
      221          // selectDigit toggles off when invoked with the curre
           ntly-active digit,
     ...
      230          }
      231          assertEquals(81, s.correctCount)
      232          assertTrue(s.isMostlySolved(10))
      233 -        // Replace one correct entry with a wrong digit. Mostl
          -y-solved                                                      
      234 -        // should *still* be true — auto-solve will overwrite 
          -the wrong                                                     
      235 -        // value when it runs.                                
      233 +        // Introduce one incorrect entry: pick a non-given cel
          +l, clear it,                                                  
      234 +        // and place a wrong value.                           
      235          val wrongTarget = (0 until 81).first { givens[it] == 0
            }
      236          val wr = wrongTarget / 9
      237          val wc = wrongTarget % 9
     ...
      241          s = GameReducer.selectDigit(s, wrong)
      242          s = GameReducer.tapCell(s, wr, wc)
      243          assertTrue(s.hasIncorrectEntry)
      245 -        assertTrue(s.isMostlySolved(10), "wrong entries no lon
          -ger block mostly-solved")                                     
      244 +        assertFalse(s.isMostlySolved(10), "any incorrect entry
          + must block mostly-solved")                                   
      245      }
      246  
      247      @Test
      249 -    fun `applySolve overwrites incorrect non-given cells with 
          -the solution`() {                                             
      250 -        var s = initial()                                     
      251 -        // Place a wrong digit at (0,2): solution = 4, place 7
          -.                                                             
      252 -        s = GameReducer.selectDigit(s, 7)                     
      253 -        s = GameReducer.tapCell(s, 0, 2)                      
      254 -        assertEquals(7, s.cellAt(0, 2).value)                 
      255 -        s = GameReducer.applySolve(s)                         
      256 -        assertEquals(solution[2], s.cellAt(0, 2).value)       
      257 -        assertEquals(CellColor.Solve, s.cellAt(0, 2).valueColo
          -r)                                                            
      258 -    }                                                         
      259 -                                                              
      260 -    @Test                                                     
      248      fun `applySolve fills empty cells in Solve color and trans
           itions phase`() {
      249          var s = initial()
      250          s = GameReducer.selectDigit(s, 4)

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -8)
  ⎿  > Task :domain:compileTestJava NO-SOURCE
     > Task :domain:testClasses UP-TO-DATE
     … +12 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Revert mostly-solved correctness check; keep auto-trigger + cell-color
      note…)
  ⎿  [claude/add-sudoku-hint-button-L1spT aee0112] Revert mostly-solved cor
     rectness check; keep auto-trigger + cell-color note
      4 files changed, 11 insertions(+), 34 deletions(-)
     … +4 lines (ctrl+o to expand)

● Bash(git fetch origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -2 &&
      git rebase origin/claude/add-sudoku-hint-button-L1spT 2>&1 | tail -3 &&
      git push 2>&1 | t…)
  ⎿   * branch            claude/add-sudoku-hint-button-L1spT -> FETCH_HEAD
        e82787a..642234d  claude/add-sudoku-hint-button-L1spT -> origin/cla
     ude/add-sudoku-hint-button-L1spT
     … +5 lines (ctrl+o to expand)

● Pushed 936ce4f.

  Sorry for misreading. Now restored:
  - isMostlySolved keeps !hasIncorrectEntry — incorrect entries still block
  auto-solve.
  - solveSingleCell and applySolve only touch empty cells (the original
  behavior).
  - Test mostly solved requires threshold AND no incorrect entries is back.

  The color-agnostic counting was already true in the original code —
  emptyCellCount checks value == null and hasIncorrectEntry compares value to
  the solution, both ignoring color. Added a comment to isMostlySolved to make
  that explicit.

  The auto-trigger behavior (state-flow collector firing onSolve() once the
  threshold is crossed) and the autoSolveStarted guard are still in place.

❯ The fireworks are mostly colored white and thus cannot be seen.               

● The blend-mode + layered glow was the culprit — BlendMode.Plus adds particle
  colors, so any overlap rapidly clips to white, and the wide outer glow made
  each particle blow out its surroundings. Stripping the additive blend and
  tightening the stack so the actual color survives:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 16 lines, removed 23 lines
      107          val h = size.height
      108          for (p in particles) {
      109              val alpha = p.life.coerceIn(0f, 1f)
      110 -            // Trail: chunkier than the previous version — wid
          -er radius                                                     
      111 -            // multiplier and slower-fading alpha so the strea
          -k reads.                                                      
      110 +            // Trail: progressively smaller + dimmer dots behi
          +nd the head.                                                  
      111 +            // Plain alpha blending so the color of the streak
          + reads true                                                   
      112 +            // instead of clipping to white.                  
      113              var step = 0
      114              for (t in p.trail) {
      115                  step++
      115 -                val trailAlpha = alpha * (1f - step.toFloat() 
          -/ MAX_TRAIL) * 0.7f                                           
      116 +                val trailAlpha = alpha * (1f - step.toFloat() 
          +/ MAX_TRAIL) * 0.45f                                          
      117                  if (trailAlpha <= 0.02f) continue
      118                  drawCircle(
      119                      color = p.color.copy(alpha = trailAlpha),
      119 -                    radius = (p.size * p.life) * 1.4f *       
      120 -                        (1f - step.toFloat() / (MAX_TRAIL + 2)
          -),                                                            
      120 +                    radius = (p.size * p.life) *              
      121 +                        (1.0f - step.toFloat() / (MAX_TRAIL + 
          +2)),                                                          
      122                      center = Offset(t.x * w, t.y * h),
      122 -                    blendMode = BlendMode.Plus,               
      123                  )
      124              }
      125 -            // Head: thick three-layer stack — wide outer glow
          -, medium                                                      
      126 -            // halo, bright core — so each particle looks like
          - a fat                                                        
      127 -            // bead instead of a thin dot.                    
      125 +            // Head: a soft outer halo at lower alpha plus a s
          +olid core.                                                    
      126 +            // Two layers, both using the particle's actual co
          +lor — keeps                                                   
      127 +            // the burst bead-shaped and the color readable.  
      128              val cx = p.x * w
      129              val cy = p.y * h
      130              drawCircle(
      131 -                color = p.color.copy(alpha = alpha * 0.3f),   
      132 -                radius = p.size * p.life * 3.4f,              
      131 +                color = p.color.copy(alpha = alpha * 0.35f),  
      132 +                radius = p.size * p.life * 1.8f,              
      133                  center = Offset(cx, cy),
      134 -                blendMode = BlendMode.Plus,                   
      134              )
      135              drawCircle(
      137 -                color = p.color.copy(alpha = alpha * 0.55f),  
      138 -                radius = p.size * p.life * 2.0f,              
      139 -                center = Offset(cx, cy),                      
      140 -                blendMode = BlendMode.Plus,                   
      141 -            )                                                 
      142 -            drawCircle(                                       
      136                  color = p.color.copy(alpha = alpha),
      144 -                radius = p.size * p.life * 1.35f,             
      137 +                radius = p.size * p.life,                     
      138                  center = Offset(cx, cy),
      146 -                blendMode = BlendMode.Plus,                   
      139              )
      140              if (p.sparkle) {
      141 +                // A small bright dot — same color, just brigh
          +ter — so                                                      
      142 +                // we don't wash everything out toward pure wh
          +ite.                                                          
      143                  drawCircle(
      150 -                    color = Color.White.copy(alpha = alpha * 0
          -.95f),                                                        
      151 -                    radius = (p.size * p.life) * 0.6f,        
      144 +                    color = p.color.copy(alpha = (alpha + 0.2f
          +).coerceAtMost(1f)),                                          
      145 +                    radius = (p.size * p.life) * 0.4f,        
      146                      center = Offset(cx, cy),
      153 -                    blendMode = BlendMode.Plus,               
      147                  )
      148              }
      149          }

❯ Also the fireworks should only last 4 seconds before returning to the new     
  game screen                                                                   

● Drop the BlendMode import since it's no longer used, lower the white-color
  probability, and shorten the firework duration:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Removed 1 line
       8  import androidx.compose.runtime.remember
       9  import androidx.compose.ui.Modifier
      10  import androidx.compose.ui.geometry.Offset
      11 -import androidx.compose.ui.graphics.BlendMode                  
      11  import androidx.compose.ui.graphics.Color
      12  import kotlinx.coroutines.delay
      13  import kotlinx.coroutines.isActive

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/ui/game/CelebrationOverlay.kt)
  ⎿  Added 4 lines, removed 2 lines
      241  }
      242  
      243  private fun pickColor(rng: Random, primary: Color, secondary: 
           Color): Color = when {
      244 -    rng.nextFloat() < 0.18f -> Color.White                    
      245 -    rng.nextFloat() < 0.45f -> secondary                      
      244 +    // Keep white rare so colors stay readable. Most particles
          + take the                                                     
      245 +    // burst's primary color, a chunk take its secondary, a fe
          +w are white.                                                  
      246 +    rng.nextFloat() < 0.06f -> Color.White                    
      247 +    rng.nextFloat() < 0.30f -> secondary                      
      248      else -> primary
      249  }
      250  

● Now the 4-second firework duration. The current firework_duration defaults to
  120000 ms (2 minutes) in config, and GameViewModel.onSolve waits for
  config.fireworkDuration before exiting. Drop the default to 4000:

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
       6      val solveCriteria: Int = 10,
       7      val solveSpeed: Long = 500,
       8      val fireworkSpeed: Float = 4f,
       9 -    val fireworkDuration: Long = 120_000,                      
       9 +    val fireworkDuration: Long = 4_000,                        
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12      val sweepCellMs: Long = 150,

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
      35                  solveCriteria = map["solve_criteria"]?.toIntOrN
          ull() ?: 10,
      36                  solveSpeed = map["solve_speed"]?.toLongOrNull()
           ?: 500,
      37                  fireworkSpeed = map["firework_speed"]?.toFloatO
          rNull() ?: 4f,
      38 -                fireworkDuration = map["firework_duration"]?.to
         -LongOrNull() ?: 120_000,                                       
      38 +                fireworkDuration = map["firework_duration"]?.to
         +LongOrNull() ?: 4_000,                                         
      39                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      40                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      41                  sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
          l() ?: 150,

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 4 lines, removed 3 lines
      13  #                 moving particles. Default baseline is 1.0.
      14  firework_speed: 4.0
      15  
      16 -# firework_duration: Total celebration duration in milliseconds
         -.                                                              
      17 -#                    Firework bursts are spread evenly across t
         -his window.                                                    
      18 -firework_duration: 120000                                      
      16 +# firework_duration: Total celebration duration in milliseconds
         + before                                                        
      17 +#                    returning to the new game screen. Firework
         + bursts                                                        
      18 +#                    are spread evenly across this window.     
      19 +firework_duration: 4000                                        
      20  
      21  # hint_step_ms: Milliseconds between each digit step (1..9) of 
          the hint
      22  #               animation sweep. Lower = faster.

● Bash(./gradlew :domain:test -PincludeApp=false --no-daemon 2>&1 | tail -8)
  ⎿  > Task :domain:compileTestKotlin UP-TO-DATE
     > Task :domain:compileTestJava NO-SOURCE
     > Task :domain:processTestResources NO-SOURCE
     … +5 lines (ctrl+o to expand)

● Bash(git add -A && git commit -m "$(cat <<'EOF'
      Stop fireworks from blowing out to white; cap duration at 4s…)
  ⎿  [claude/add-sudoku-hint-button-L1spT 68e83de] Stop fireworks from blow
     ing out to white; cap duration at 4s
      3 files changed, 26 insertions(+), 31 deletions(-)
     … +8 lines (ctrl+o to expand)
● Pushed a33613b.

  Fireworks color fix:
  - Dropped BlendMode.Plus (the actual culprit). Additive blending sums RGB
  channels of every overlapping particle and clips at 1.0 → dense centers were
  always washing to white.
  - Head halo went from a 3-layer stack (30% / 55% / 100% alpha at 3.4× / 2.0× /
  1.35× radius) to 2 layers (35% at 1.8×, 100% at 1.0×) so a single particle no
  longer triple-stacks itself bright.
  - pickColor: white probability 18% → 6%, secondary chance simplified.
  - Sparkle: was a pure-white center dot, now a brighter same-color dot — keeps
  the twinkle without injecting more white.

  Firework duration: firework_duration default 120000 → 4000 ms in both
  GameConfig and config.yaml. The celebration overlay's existing LaunchedEffect
  already respects this window, and GameViewModel.onSolve
  delay(config.fireworkDuration) before flipping _exitSignal — so the user is
  back on the menu four seconds after the last cell fills.

❯ Make it 5 seconds of fireworks                                                

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
       6      val solveCriteria: Int = 10,
       7      val solveSpeed: Long = 500,
       8      val fireworkSpeed: Float = 4f,
       9 -    val fireworkDuration: Long = 4_000,                        
       9 +    val fireworkDuration: Long = 5_000,                        
      10      val hintStepMs: Long = 500,
      11      val hintFlashMs: Long = 600,
      12      val sweepCellMs: Long = 150,

● Update(/home/user/soduku-trainer/app/src/main/kotlin/com/dividetask/sudokutrai
        ner/data/GameConfig.kt)
  ⎿  Added 1 line, removed 1 line
      35                  solveCriteria = map["solve_criteria"]?.toIntOrN
          ull() ?: 10,
      36                  solveSpeed = map["solve_speed"]?.toLongOrNull()
           ?: 500,
      37                  fireworkSpeed = map["firework_speed"]?.toFloatO
          rNull() ?: 4f,
      38 -                fireworkDuration = map["firework_duration"]?.to
         -LongOrNull() ?: 4_000,                                         
      38 +                fireworkDuration = map["firework_duration"]?.to
         +LongOrNull() ?: 5_000,                                         
      39                  hintStepMs = map["hint_step_ms"]?.toLongOrNull(
          ) ?: 500,
      40                  hintFlashMs = map["hint_flash_ms"]?.toLongOrNul
          l() ?: 600,
      41                  sweepCellMs = map["sweep_cell_ms"]?.toLongOrNul
          l() ?: 150,

● Update(/home/user/soduku-trainer/app/src/main/assets/config.yaml)
  ⎿  Added 1 line, removed 1 line
      16  # firework_duration: Total celebration duration in milliseconds
           before
      17  #                    returning to the new game screen. Firework
           bursts
      18  #                    are spread evenly across this window.
      19 -firework_duration: 4000                                        
      19 +firework_duration: 5000                                        
      20  
      21  # hint_step_ms: Milliseconds between each digit step (1..9) of 
          the hint
      22  #               animation sweep. Lower = faster.

● Bash(git add -A && git commit -m "Bump firework duration to 5 seconds" && git
      fetch origin claude/add-sudoku-hint-button-L1spT 2>&1 | tail -2 && git
      rebase origin/cl…)
  ⎿  [claude/add-sudoku-hint-button-L1spT a7eeceb] Bump firework duration t
     o 5 seconds
      2 files changed, 3 insertions(+), 3 deletions(-)
     … +8 lines (ctrl+o to expand)

● Pushed cfb7ba9. firework_duration is now 5000 ms in both the YAML default and
  the GameConfig fallback.

● Session resumed without branch: Failed to checkout branch 
  'claude/add-sudoku-hint-button-L1spT'
