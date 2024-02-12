import sys
import math
from collections import deque 

# ======================================
# pathfinding
# ======================================
# checks if coordinate is in bounds
def in_bounds(x, y, width, height):
    return 0 <= x and x < width and 0 <= y and y < height

def get_ordered_list(points, x, y):
   points.sort(key = lambda p: (p[0] - x)**2 + (p[1] - y)**2)
   return points

def return_adjacent_edges(v, w, h, arena_map, ex, ey, gx, gy):
    edges = []
    x, y = v[0], v[1]
    for i in range(-1, 2):
        for j in range(-1, 2):
            if i != 0 or j != 0:
                tx = x + i
                ty = y + j
                if in_bounds(tx, ty, w, h) and ((tx == ex and ty == ey) or (arena_map[tx][ty][0] == -1 and arena_map[tx][ty][1] == 0)):
                    edges.append((tx, ty))
    return get_ordered_list(edges, gx, gy)

# bfs
def find_path_bfs(weight, height, arena_map, x, y, tx, ty):
    q = deque([(tx,ty)])
    visited = {(tx,ty): (-1,-1)}

    while q:
        current_node = q.popleft()
        if current_node == (x,y):
            break

        next_nodes = return_adjacent_edges(current_node, weight, height, arena_map, x, y, tx, ty)
        for next_node in next_nodes:
            if next_node not in visited:
                q.append(next_node)
                visited[next_node] = current_node
    if (x,y) in visited:
        return visited[(x,y)]
    else:
        return (-1,-1)

# finds the closest target and gives first step to it
def find_step_to_closest(x, y, target, arena_map, w, h):
    tx, ty, td = -1, -1, w*h+1
    for t in target:
        newd = math.dist([x,y],[t[0],t[1]])
        if td > newd:
            tx, ty, td = t[0], t[1], newd
    if tx != -1 and ty != -1:
        v0, v1 = find_path_bfs(w, h, arena_map, x, y, tx, ty)
        if v0 != -1 and v1 != -1:
            return v0, v1, tx, ty
        else:
            return -1, -1, tx, ty
    else:
        return -1, -1, -1, -1

# ======================================
# general
# ======================================
# creates order text
def action_or_step(x, y, sx, sy, tx, ty, reach, order, miners, lumberjacks, builders, fighters, p_unitsXY):
    if tx != -1 and ty != -1:
        if abs(x-tx) <= reach and abs(y-ty) <= reach:
            return order + " " + str(x) + " " + str(y) + " " + str(tx) + " " + str(ty) + ";"
        elif sx != -1 and sy != -1 and (sx, sy) not in p_unitsXY:
            if (x, y) in miners:
                miners.remove((x, y))
                miners.append((sx, sy))
                return "MOVE " + str(x) + " " + str(y) + " " + str(sx) + " " + str(sy) + ";"
            elif (x, y) in lumberjacks:
                lumberjacks.remove((x, y))
                lumberjacks.append((sx, sy))
                return "MOVE " + str(x) + " " + str(y) + " " + str(sx) + " " + str(sy) + ";"
            elif (x, y) in builders:
                builders.remove((x, y))
                builders.append((sx, sy))
                return "MOVE " + str(x) + " " + str(y) + " " + str(sx) + " " + str(sy) + ";"
            elif (x, y) in fighters:
                fighters.remove((x, y))
                fighters.append((sx, sy))
                return "MOVE " + str(x) + " " + str(y) + " " + str(sx) + " " + str(sy) + ";"
            else:
                return "MOVE " + str(x) + " " + str(y) + " " + str(sx) + " " + str(sy) + ";"
    return ""
	
# finds the best tile to build at in range
def find_tile(x, y, reach, arena_map, width, height):
    for i in range(2*reach + 1):
        for j in range(2*reach + 1):
            if x < 16 and y < 9:
                temp_x, temp_y = x+reach-i, y+reach-j
                if in_bounds(temp_x, temp_y, width, height) and arena_map[temp_x][temp_y][0] == -1 and arena_map[temp_x][temp_y][1] == 0:
                    return temp_x, temp_y
					
            elif x < 16 and y >= 9:
                temp_x, temp_y = x+reach-i, y-reach+j
                if in_bounds(temp_x, temp_y, width, height) and arena_map[temp_x][temp_y][0] == -1 and arena_map[temp_x][temp_y][1] == 0:
                    return temp_x, temp_y
					
            elif x >= 16 and y < 9:
                temp_x, temp_y = x-reach+i, y+reach-j
                if in_bounds(temp_x, temp_y, width, height) and arena_map[temp_x][temp_y][0] == -1 and arena_map[temp_x][temp_y][1] == 0:
                    return temp_x, temp_y
					
            else:
                temp_x, temp_y = x-reach+i, y-reach+j
                if in_bounds(temp_x, temp_y, width, height) and arena_map[temp_x][temp_y][0] == -1 and arena_map[temp_x][temp_y][1] == 0:
                    return temp_x, temp_y
    return -1, -1

# ======================================
# main
# ======================================
# Order your units for map domination!

w, h = [int(i) for i in input().split()]
entity_types = [] # 0 - castle, 1 - barracks, 2 - worker, 3 - light, 4 - heavy, 5 - ranged
for i in range(6):
    # entity_type, max_hp, reach, attack, step, gold_cost, wood_cost
    entity_types.append([int(j) for j in input().split()])

turn = 1
gold = 0
wood = 0
enemyGold = 0
enemyWood = 0

miners = []
lumberjacks = []
builders = []
fighters = []

# game loop
while True:
    walls, mines, forests, enemy, player = [], [], [], [], []
    p_unitsXY, p_castles, p_barracks, p_workers, p_ranged = [], [], [], [], []

    arena_map = []
    for i in range(w):
        line = []
        for j in range(h):
            line.append([-1, 0, 0]) # ownerID, entityType, health
        arena_map.append(line)
    
    n, gold, wood, enemyGold, enemyWood = [int(i) for i in input().split()]
    for i in range(n):
        # x, y, owner, entity_type, health
        entity = [int(j) for j in input().split()] 
        
        arena_map[entity[0]][entity[1]][0] = entity[2] #ownerID
        arena_map[entity[0]][entity[1]][1] = entity[3] #entityType
        arena_map[entity[0]][entity[1]][2] = entity[4] #health
        
        if entity[2] == -1 and (entity[3] == 1 or entity[3] == 4):
            walls.append(entity)
        elif entity[2] == -1 and entity[3] == 2:
            mines.append(entity)
        elif entity[2] == -1 and entity[3] == 3:
            forests.append(entity)
        elif entity[2] == 0:
            player.append(entity)
            if entity[3] == 0:
                p_castles.append(entity)
            elif entity[3] == 1:
                p_barracks.append(entity)
            elif entity[3] == 2:
                p_workers.append(entity)
                p_unitsXY.append((entity[0],entity[1]))
                if not ((entity[0],entity[1]) in miners or (entity[0],entity[1]) in lumberjacks or (entity[0],entity[1]) in fighters):
                    if len(miners) < 3:
                        miners.append((entity[0],entity[1]))
                    elif len(lumberjacks) < 3:
                        lumberjacks.append((entity[0],entity[1]))
                    elif len(builders) < 1:
                        builders.append((entity[0],entity[1]))
                    else:
                        fighters.append((entity[0],entity[1]))

            elif entity[3] == 5:
                p_ranged.append(entity)
                p_unitsXY.append((entity[0],entity[1]))

        elif entity[2] == 1:
            enemy.append(entity)
    
    for e in miners:
        if e not in p_unitsXY:
            miners.remove(e)
    for e in lumberjacks:
        if e not in p_unitsXY:
            lumberjacks.remove(e)
    for e in builders:
        if e not in p_unitsXY:
            builders.remove(e)
    for e in fighters:
        if e not in p_unitsXY:
            fighters.remove(e)
    
	
    orders = ""
    for e in p_castles:
        if len(p_workers) < 15 and gold >= entity_types[2][5] and wood >= entity_types[2][6]:
            tx, ty = find_tile(e[0], e[1], entity_types[0][2], arena_map, w, h)
            if tx != -1 and ty != -1:
                orders += "TRAIN " + str(e[0]) + " " + str(e[1]) + " " + str(tx) + " " + str(ty) + " WORKER;"
                gold -= entity_types[2][5]
                wood -= entity_types[2][6]
    
    for e in p_barracks:
        if len(p_ranged) < 15 and gold >= entity_types[5][5] and wood >= entity_types[5][6]:
            tx, ty = find_tile(e[0], e[1], entity_types[1][2], arena_map, w, h)
            if tx != -1 and ty != -1:
                orders += "TRAIN " + str(e[0]) + " " + str(e[1]) + " " + str(tx) + " " + str(ty) + " RANGED;"
                gold -= entity_types[5][5]
                wood -= entity_types[5][6]
                
    for i in range(len(p_ranged)):
        e = p_ranged[i]
        sx, sy, tx, ty = find_step_to_closest(e[0], e[1], enemy, arena_map, w, h)
        orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[5][2], "ATTACK", miners, lumberjacks, builders, fighters, p_unitsXY)
    
    for i in range(len(p_workers)):
        e = p_workers[i]
        # miners
        if (e[0], e[1]) in miners:
            if len(mines):
                sx, sy, tx, ty = find_step_to_closest(e[0], e[1], mines, arena_map, w, h)
                orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "HARVEST", miners, lumberjacks, builders, fighters, p_unitsXY)
            else:
                sx, sy, tx, ty = find_step_to_closest(e[0], e[1], enemy, arena_map, w, h)
                orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "ATTACK", miners, lumberjacks, builders, fighters, p_unitsXY)

        # lumberjacks
        elif (e[0], e[1]) in lumberjacks:
            if len(forests):
                sx, sy, tx, ty = find_step_to_closest(e[0], e[1], forests, arena_map, w, h)
                orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "HARVEST", miners, lumberjacks, builders, fighters, p_unitsXY)
            
            else:
                sx, sy, tx, ty = find_step_to_closest(e[0], e[1], enemy, arena_map, w, h)
                orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "ATTACK", miners, lumberjacks, builders, fighters, p_unitsXY)

        # builders
        elif (e[0], e[1]) in builders:
            if len(p_barracks) < 1 and gold >= entity_types[1][5] and wood >= entity_types[1][6]:
                tx, ty = find_tile(e[0], e[1], entity_types[2][2], arena_map, w, h)
                if tx != -1 and ty != -1:
                    orders += "BUILD " + str(e[0]) + " " + str(e[1]) + " " + str(tx) + " " + str(ty) + " BARRACKS;"
                    gold -= entity_types[1][5]
                    wood -= entity_types[1][6]
            elif len(p_castles) < 2 and gold >= entity_types[0][5] and wood >= entity_types[0][6]:
                tx, ty = find_tile(e[0], e[1], entity_types[2][2], arena_map, w, h)
                if tx != -1 and ty != -1:
                    orders += "BUILD " + str(e[0]) + " " + str(e[1]) + " " + str(tx) + " " + str(ty) + " CASTLE;"
                    gold -= entity_types[0][5]
                    wood -= entity_types[0][6]
            else:
                sx, sy, tx, ty = find_step_to_closest(e[0], e[1], enemy, arena_map, w, h)
                orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "ATTACK", miners, lumberjacks, builders, fighters, p_unitsXY)
			
		# fighters	
        else:
            sx, sy, tx, ty = find_step_to_closest(e[0], e[1], enemy, arena_map, w, h)
            orders += action_or_step(e[0], e[1], sx, sy, tx, ty, entity_types[2][2], "ATTACK", miners, lumberjacks, builders, fighters, p_unitsXY)

    orders += "WAIT"
    print(orders)
    turn += 1
    
    # Write an action using print
    # To debug: print("Debug messages...", file=sys.stderr, flush=True)


