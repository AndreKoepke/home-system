import {WeatherValuePipe} from './weahter-value.pipe';

describe('WeahterValuePipe', () => {
  it('create an instance', () => {
    const pipe = new WeatherValuePipe();
    expect(pipe).toBeTruthy();
  });
});
