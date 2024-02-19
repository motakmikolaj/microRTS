import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';
import { AnimatedEventModule } from './animations/AnimatedEventModule.js';
import { TooltipModule } from './tooltip-module/TooltipModule.js';
import { EndScreenModule } from './endscreen-module/EndScreenModule.js';

export const modules = [
  GraphicEntityModule,
  AnimatedEventModule,
  TooltipModule,
  EndScreenModule
];
export const playerColors = [
  '#ff0000',  // solid red
  '#22a1e4', // curious blue
  '#6ac371', // mantis green
  '#9975e2', // medium purple
  '#de6ddf', // lavender pink
  '#ff8f16', // west side orange  
  '#ff1d5c', // radical red
  '#3ac5ca' // scooter blue
  
];
