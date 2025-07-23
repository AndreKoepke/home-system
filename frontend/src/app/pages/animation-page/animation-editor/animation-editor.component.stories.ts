import type {Meta, StoryObj} from '@storybook/angular';
import {AnimationEditorComponent} from "./animation-editor.component";
import {AnimationDto} from "../../../models/animation.dto";


//👇 This default export determines where your story goes in the story list
const meta: Meta<AnimationEditorComponent> = {
  component: AnimationEditorComponent,
  argTypes: {
    createStep: {action: 'outputChange'},
  }
};

export default meta;
type Story = StoryObj<AnimationEditorComponent>;

export const EmptyAnimation: Story = {
  args: {
    animation: {id: 'empty', steps: [], name: 'test'} as AnimationDto
  },
};


export const SingleStep: Story = {
  args: {
    animation: {
      id: 'empty',
      name: 'test',
      steps: [{
        id: 'firstStep',
        sortOrder: 1,
        actionDescription: 'Allet aus',
        affectedLight: 'Allet'
      }]
    } as AnimationDto
  },
};
