import type {Meta, StoryObj} from '@storybook/angular';
import {TimerPageComponent} from "./timer-page.component";
import {TimerConfig} from "../../models/timer-config.dto";


//👇 This default export determines where your story goes in the story list
const meta: Meta<TimerPageComponent> = {
  component: TimerPageComponent,
  argTypes: {}
};

export default meta;
type Story = StoryObj<TimerPageComponent>;


export const Single: Story = {
  args: {
    // @ts-ignore
    devices: [{id: 'test', name: 'Lampe A'}, {id: 'test2', name: 'Lampe B'},],
    timerConfigs: [{
      id: '123',
      name: 'test',
      turnOnAt: '08:00',
      turnOffAt: '20:00',
      lights: ['Lampe A']
    }] as TimerConfig[]
  },
};
