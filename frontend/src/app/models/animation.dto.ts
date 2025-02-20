export interface AnimationDto {
  id: string;
  name: string;
  steps: Step[];
}

export interface Step {
  id: string;
  sortOrder: number;
  actionDescription: string;
  affectedLight?: string;
}
