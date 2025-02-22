export interface AnimationDto {
  id: string;
  name: string | undefined;
  steps: Step[];
}

export interface Step {
  id: string;
  sortOrder: number;
  actionDescription: string;
  affectedLight?: string;
}
